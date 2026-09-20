/*
 * Mini - an ASM-based class transformer reminiscent of MalisisCore and Mixin
 * 
 * The MIT License
 *
 * Copyright (c) 2017-2021 Una Thompson (unascribed) and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nilloader.api.lib.mini;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.cadixdev.bombe.type.signature.MethodSignature;
import org.objectweb.asm.tree.*;

import nilloader.NilAgent;
import nilloader.NilLoaderLog;
import nilloader.api.ASMTransformer;
import nilloader.api.lib.mini.annotation.Patch;

public abstract class MiniTransformer extends InsnBuilder implements ASMTransformer {
	
	private interface PatchMethod {
		boolean patch(PatchContext ctx) throws Throwable;
	}
	
	private final String classTargetName;
	private final Map<String, List<PatchMethod>> methods = new HashMap<String, List<PatchMethod>>();
	private final Set<String> requiredMethods = new HashSet<String>();
	public MiniTransformer() {
		super(Optional.ofNullable(NilAgent.getActiveMappings(NilAgent.getActiveMod())));
		Patch.Class classAnn = getClass().getAnnotation(Patch.Class.class);
		String className = classAnn.value().replace('.', '/');
		classTargetName = remapType(className);
		if (!className.equals(classTargetName)) {
			$$internal$logDebug("Retargeted {} from {} to {}", getClass().getSimpleName(), className, classTargetName);
		}
		for (final Method m : getClass().getMethods()) {
			final String name = m.getName();
			for (final Patch.Method a : m.getAnnotationsByType(Patch.Method.class)) {
				MethodSignature sig = MethodSignature.of(a.value());
				String desc = remapMethod(className, sig.getName(), sig.getDescriptor().toString())+remapMethodDesc(sig.getDescriptor().toString());
				if (!a.value().equals(desc)) {
					$$internal$logDebug("Retargeted {}.{} from {} to {}", getClass().getSimpleName(), name, a.value(), desc);
				}
				if (!methods.containsKey(desc)) {
					methods.put(desc, new ArrayList<PatchMethod>());
				}
				final boolean frames = m.getAnnotation(Patch.Method.AffectsControlFlow.class) != null;
				methods.get(desc).add(new PatchMethod() {
					@Override
					public boolean patch(PatchContext ctx) throws Throwable {
						try {
							m.invoke(MiniTransformer.this, ctx);
						} catch (InvocationTargetException e) {
							throw e.getCause();
						}
						return frames;
					}
					@Override
					public String toString() {
						return name;
					}
				});
				if (m.getAnnotation(Patch.Method.Optional.class) == null) {
					requiredMethods.add(desc);
				}
			}
		}
	}
	
	public String getClassTargetName() {
		return classTargetName;
	}
	
	/**
	 * Override point to perform modifications to a class that Mini's method patching is not sufficient
	 * for. This can be used to add entirely new methods, add interfaces, add fields, etc.
	 * <p>
	 * <b>Mini and NilGradle will not be aware of what you are doing in this method, and therefore
	 * <i>no remapping will occur</i>!</b>
	 * @return {@code true} if frames need to be computed, equivalent to having any successful
	 * 		patch method annotated with {@link nilloader.api.lib.mini.annotation.Patch.Method.AffectsControlFlow @Patch.Method.AffectsControlFlow}
	 */
	protected boolean modifyClassStructure(ClassNode clazz) {
		return false;
	}
	
	public Set<String> getTargets() {
		return Collections.singleton(classTargetName);
	}
	
	@Override
	public final boolean transform(ClassLoader loader, ClassNode clazz) {
		String className = clazz.name.replace('.', '/');
		if (!classTargetName.equals(className)) return false;
		
		boolean frames = modifyClassStructure(clazz);
		
		List<String> foundMethods = new ArrayList<String>();
		Set<String> requiredsNotSeen = new HashSet<String>(requiredMethods.size());
		requiredsNotSeen.addAll(requiredMethods);
		
		for (MethodNode mn : clazz.methods) {
			String name = mn.name+mn.desc;
			foundMethods.add(name);
			List<PatchMethod> li = methods.get(name);
			if (li != null) {
				for (PatchMethod pm : li) {
					try {
						PatchContext ctx = new PatchContext(mn, this.mappings);
						frames |= pm.patch(ctx);
						ctx.finish();
					} catch (Throwable t) {
						throw new Error("Failed to patch "+className+"."+mn.name+mn.desc+" via "+pm, t);
					}
					$$internal$logDebug("[{}] Successfully transformed {}.{}{} via {}", getClass().getName(), className, mn.name, mn.desc, pm);
				}
			}
			requiredsNotSeen.remove(name);
		}
		
		if (!requiredsNotSeen.isEmpty()) {
			StringBuilder msg = new StringBuilder();
			msg.append(requiredsNotSeen.size());
			msg.append(" required method");
			msg.append(requiredsNotSeen.size() == 1 ? " was" : "s were");
			msg.append(" not found while patching ");
			msg.append(className);
			msg.append("!");
			for (String name : requiredsNotSeen) {
				msg.append(" ");
				msg.append(name);
				msg.append(",");
			}
			msg.deleteCharAt(msg.length()-1);
			msg.append("\nThe following methods were found:");
			for (String name : foundMethods) {
				msg.append(" ");
				msg.append(name);
				msg.append(",");
			}
			msg.deleteCharAt(msg.length()-1);
			String msgS = msg.toString();
			throw new Error(msgS);
		}
		
		return frames;
	}
	
	@Override
	public final boolean canTransform(ClassLoader loader, String className) {
		return classTargetName.equals(className.replace('.', '/'));
	}
	
	@Override @Deprecated
	public final byte[] transform(ClassLoader loader, String className, byte[] originalData) {
		return ASMTransformer.super.transform(loader, className, originalData);
	}
	
	@Override @Deprecated
	public final byte[] transform(String className, byte[] originalData) {
		return ASMTransformer.super.transform(className, originalData);
	}
	
	protected void $$internal$logDebug(String fmt, Object... params) {
		NilLoaderLog.log.debug(fmt, params);
	}

	protected void $$internal$logError(String fmt, Object... params) {
		NilLoaderLog.log.error(fmt, params);
	}

}
