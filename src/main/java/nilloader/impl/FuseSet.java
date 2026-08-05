package nilloader.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public final class FuseSet<E> extends AbstractSet<E> implements Fuseable {
	
	private static final MethodHandle burner;
	static {
		MethodHandle burnerTmp;
		try {
			burnerTmp = MethodHandles.publicLookup().findStatic(Set.class, "copyOf", MethodType.methodType(Set.class, Set.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			try {
				burnerTmp = MethodHandles.publicLookup().findStatic(Collections.class, "unmodifiableSet", MethodType.methodType(Set.class, Set.class));
			} catch (NoSuchMethodException | IllegalAccessException e2) {
				throw new AssertionError();
			}
		}
		burner = burnerTmp;
	}

	private Set<E> impl = new LinkedHashSet<>();

	@Override
	public int size() {
		return impl.size();
	}

	@Override
	public boolean isEmpty() {
		return impl.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return impl.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return impl.iterator();
	}

	@Override
	public boolean add(E e) {
		return impl.add(e);
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
			impl = (Set<E>)burner.invokeExact(impl);
			for (E e : this) {
				if (e instanceof Fuseable) {
					((Fuseable)e).burn();
				}
			}
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
	}
	
}
