class Factorial{
    public static void main(String[] a){
	System.out.println(new Fac().ComputeFac(10));
    }
}

class Fac {
	int i;
	int b;
	int a;
	int y;
	
    public int ComputeFac(int num)
	{
		boolean boo;
		int num_aux;
		int kool;
		Fac fac;
		
		boo = true;
		boo = false;
		boo = !boo;
		
		this.b = 6;
		
		if (num < 1)
	    	num_aux = 3 ;
		else if(num != 7 && true){}
		else if(num == 5 || false){}
		else 
	    	num_aux = num * (this.ComputeFac(num-1)) ;
		return num_aux ;
    }

}
