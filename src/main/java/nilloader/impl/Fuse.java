package nilloader.impl;

public final class Fuse implements Fuseable {

	private boolean burnt = false;
	
	@Override
	public void burn() {
		burnt = true;
	}
	
	public boolean isBurnt() {
		return burnt;
	}
	
}
