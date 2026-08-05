package nilloader.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FuseList<E> extends AbstractList<E> implements Fuseable {
	
	private static final MethodHandle burner;
	static {
		MethodHandle burnerTmp;
		try {
			burnerTmp = MethodHandles.publicLookup().findStatic(List.class, "copyOf", MethodType.methodType(List.class, List.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			try {
				burnerTmp = MethodHandles.publicLookup().findStatic(Collections.class, "unmodifiableList", MethodType.methodType(List.class, List.class));
			} catch (NoSuchMethodException | IllegalAccessException e2) {
				throw new AssertionError();
			}
		}
		burner = burnerTmp;
	}

	private List<E> impl = new CopyOnWriteArrayList<>();
	
	

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
	public E get(int index) {
		return impl.get(index);
	}

	@Override
	public E set(int index, E element) {
		return impl.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		impl.add(index, element);
	}

	@Override
	public E remove(int index) {
		return impl.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return impl.indexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return impl.listIterator();
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
			impl = (List<E>)burner.invokeExact(impl);
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
