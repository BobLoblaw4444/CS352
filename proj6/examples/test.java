class Factorial{
    public static void main(String[] a){
	System.out.println(new Fac().ComputeFac(10));
    }
}

class Fac {
	int i;
	int b;
	
    public int ComputeFac(int num)
	{
		int num_aux ;
		if (num < 1)
	    	num_aux = 1 ;
		else if(num != 7 && true){}
		else if(num == 5 || false){}
		else 
	    	num_aux = num * (this.ComputeFac(num-1)) ;
		return num_aux ;
    }

}
