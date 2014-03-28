class Main {
		public static void main(String[] args) {
				System.out.println(4);
		}
}
class Foo {
		int a;
		public int foo(int[] b) {
			return 1;
		}
}
class Foo2 extends Foo {
		int b;
		public int foo2(int c) {
			return 2;
		}
}
class Foo3 extends Foo2 {
		int d;
		public int foo3(int e) {
			Foo f;
			Foo2 f2;
			Foo3 f3;
			f2.foo3();
			f.foo2();
			return 2;
		}
}
