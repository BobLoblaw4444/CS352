class Main {
		public static void main(String[] args) {
				new Foo();
		}
}

class Foo {
	public int foo(int a, int b) {
		Foo f;
		f.foo2();
		return 1;
	}
	public int foo2(int c) {
		return 2;
	}
}
