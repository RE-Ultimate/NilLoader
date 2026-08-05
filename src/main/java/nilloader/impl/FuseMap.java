package nilloader.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class FuseMap<K, V> extends AbstractMap<K, V> implements Fuseable {
	
	private static final MethodHandle burner;
	static {
		MethodHandle burnerTmp;
		try {
			burnerTmp = MethodHandles.publicLookup().findStatic(Map.class, "copyOf", MethodType.methodType(Map.class, Map.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			try {
				burnerTmp = MethodHandles.publicLookup().findStatic(Collections.class, "unmodifiableMap", MethodType.methodType(Map.class, Map.class));
			} catch (NoSuchMethodException | IllegalAccessException e2) {
				throw new AssertionError();
			}
		}
		burner = burnerTmp;
	}

	private Map<K, V> impl = new LinkedHashMap<>();
	
	@Override
	public Set<Entry<K, V>> entrySet() {
		return impl.entrySet();
	}

	@Override
	public int size() {
		return impl.size();
	}

	@Override
	public boolean containsKey(Object key) {
		return impl.containsKey(key);
	}

	@Override
	public V get(Object key) {
		return impl.get(key);
	}

	@Override
	public V put(K key, V value) {
		return impl.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return impl.remove(key);
	}

	@Override
	public void clear() {
		impl.clear();
	}
	
	@Override
	public String toString() {
		return impl.toString();
	}
	
	@Override
	public int hashCode() {
		return impl.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return impl.equals(o);
	}
	
	@Override
	public void burn() {
		try {
			impl = (Map<K, V>)burner.invokeExact(impl);
			for (V v : values()) {
				if (v instanceof Fuseable) {
					((Fuseable)v).burn();
				}
			}
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
	}
	
}
