package nilloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import nilloader.api.NilLogger;

class Decompiler {

	private interface VineflowerAccess {
		String decompile(String name, byte[] clazz);
	}
	
	private static final class VineflowerAccessImpl implements VineflowerAccess {

		private final NilLogger log = NilLogger.get("Vineflower");
		
		@Override
		public String decompile(String name, byte[] clazz) {
			Map<String, Object> options = new HashMap<>();
			options.put(IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH, "0");
			options.put(IFernflowerPreferences.INCLUDE_JAVA_RUNTIME, "1");
			options.put(IFernflowerPreferences.THREADS, "4");
			options.put(IFernflowerPreferences.INDENT_STRING, "\t");
			String[] contentArr = new String[] { "// decompilation failed" };
			Fernflower ff = new Fernflower(new IBytecodeProvider() {
				
				@Override
				public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
					if (externalPath != null && externalPath.endsWith(".nil/bad-fernflower-workaround/this-file-does-not-exist.class")) {
						return clazz;
					}
					throw new FileNotFoundException();
				}
			}, new IResultSaver() {
				@Override public void saveFolder(String path) {}
				@Override public void saveDirEntry(String path, String archiveName, String entryName) {}
				@Override public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
					contentArr[0] = content;
				}
				@Override public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
					contentArr[0] = content;
				}
				@Override public void createArchive(String path, String archiveName, Manifest manifest) {}
				@Override public void copyFile(String source, String path, String entryName) {}
				@Override public void copyEntry(String source, String path, String archiveName, String entry) {}
				@Override public void closeArchive(String path, String archiveName) {}
			}, options, new IFernflowerLogger() {
				
				@Override
				public void writeMessage(String message, Severity severity, Throwable t) {
					if (t instanceof FileNotFoundException) return;
					if (severity == Severity.WARN) {
						log.warn(message, t);
					} else if (severity == Severity.ERROR) {
						log.error(message, t);
					}
				}
				
				@Override
				public void writeMessage(String message, Severity severity) {
					if (severity == Severity.WARN) {
						log.warn(message);
					} else if (severity == Severity.ERROR) {
						log.error(message);
					}
				}
				
			});
			try {
				ff.addSource(new File(".nil/bad-fernflower-workaround/this-file-does-not-exist.class"));
				try {
					ff.decompileContext();
				} finally {
					ff.clearContext();
				}
				return contentArr[0];
			} catch (Throwable e) {
				log.error("Failed to decompile", e);
				return "// decompilation failed";
			}
		}
		
	}
	
	private static boolean failure = false;
	private static VineflowerAccess access = null;
	
	private enum DecompilerProvider {
		QUILTFLOWER_1_8_1(8, "1.8.1", "https://maven.quiltmc.org/repository/release/org/quiltmc/quiltflower/1.8.1/quiltflower-1.8.1.jar",
				"79hnFEabe9+TEz1KefC7gcugcmuCcz3N1Ok/80kolcs=", 874457, true),
		VINEFLOWER_1_10_1(11, "1.10.1", "https://repo1.maven.org/maven2/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar",
				"ubII5QeTtkZXprYpIGdSZhP1Sd50BfkkNiSwL0J25Ak=", 1778509, true),
		VINEFLOWER_1_11_1(17, "1.11.1", "https://repo1.maven.org/maven2/org/vineflower/vineflower/1.11.1/vineflower-1.11.1.jar",
				"phXQfdu81Ik2lnT0DkLfY5wyvpVBCJCzjxc9XB4uo5w=", 1847869, false),
		;
		public final String version, url;
		public final byte[] expectedHash;
		public final int neededJavaVersion, expectedSize;
		public final boolean nag;
		DecompilerProvider(int neededJavaVersion, String version, String url, String expectedHashB64, int expectedSize, boolean nag) {
			this.neededJavaVersion = neededJavaVersion;
			this.version = version;
			this.url = url;
			this.expectedHash = Base64.getDecoder().decode(expectedHashB64);
			this.expectedSize = expectedSize;
			this.nag = nag;
		}
	}
	
	static void initialize() {
		try {
			File dotNil = new File(".nil");
			
			DecompilerProvider dp = DecompilerProvider.QUILTFLOWER_1_8_1;
			
			try {
				String[] jver = System.getProperty("java.version").split("\\.");
				int jverI = Integer.parseInt("1".equals(jver[0]) ? jver[1] : jver[0]);
				for (DecompilerProvider opt : DecompilerProvider.values()) {
					if (opt.neededJavaVersion <= jverI) {
						dp = opt;
					}
				}
			} catch (Throwable t) {
				NilLoaderLog.log.error("Failed to parse Java version", t);
			}
			
			File vineflower = new File(dotNil, "vineflower-"+dp.version+".jar");
			if (!vineflower.exists()) {
				NilLoaderLog.log.info("Downloading Vineflower v"+dp.version+"..."+(dp.nag ? " (Update Java to use a newer version)" : ""));
				try {
					URL u = new URL(dp.url);
					dotNil.mkdirs();
					File tmp = File.createTempFile("vineflower", ".jar.part", dotNil);
					byte[] buf = new byte[4096];
					MessageDigest digest = MessageDigest.getInstance("SHA-256");
					int realSize = 0;
					try (InputStream in = u.openStream(); OutputStream out = new FileOutputStream(tmp)) {
						while (true) {
							int read = in.read(buf);
							if (read == -1) break;
							realSize += read;
							if (realSize > dp.expectedSize) throw new IOException("File is longer than expected length "+dp.expectedSize);
							out.write(buf, 0, read);
							digest.update(buf, 0, read);
						}
					}
					if (realSize < dp.expectedSize) throw new IOException("File is shorter than expected length "+dp.expectedSize);
					byte[] realHash = digest.digest();
					if (!Arrays.equals(dp.expectedHash, realHash)) {
						throw new IOException("File hash is incorrect (got "+Base64.getEncoder().encodeToString(realHash)+", but expected "+Base64.getEncoder().encodeToString(dp.expectedHash)+")");
					}
					tmp.renameTo(vineflower);
				} catch (IOException | NoSuchAlgorithmException e) {
					NilLoaderLog.log.error("Failed to download Vineflower", e);
					failure = true;
					return;
				}
			}
			NilAgent.injectToSearchPath(new JarFile(vineflower));
			access = new VineflowerAccessImpl();
		} catch (Throwable t) {
			NilLoaderLog.log.error("Failed to load Vineflower", t);
			failure = true;
			return;
		}
	}
	
	static String decompile(String name, byte[] bys) {
		try {
			if (failure) return "// decompiler failed to load";
			return access.decompile(name, bys);
		} catch (Throwable t) {
			NilLoaderLog.log.error("Failed to decompile {}", name, t);
			return "// decompilation failed";
		}
	}

}
