class Main {
		public static void main(String[] args) {
				new Foo();
		}
}

class Foo {
	int f;
	public Foo2 foo(int[] arr) {
		Foo2 f;
		return f.foogoo();
	}
}

class Foo2 extends Foo {
	int f2;
	public Foo foogoo() {
		return new Foo();
	}	
}

class Foo3 extends Foo2 {
	int f3;
	public int test() {
		return 1;
	}
}
