package link.infra.borderlessmining.util;

public class WindowBoundsHolder implements WindowBoundsGetter {
	final int x;
	final int y;
	final int width;
	final int height;

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public WindowBoundsHolder(WindowBoundsGetter src) {
		this.x = src.getX();
		this.y = src.getY();
		this.width = src.getWidth();
		this.height = src.getHeight();
	}
}
