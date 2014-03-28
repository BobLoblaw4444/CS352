class Factorial{
    public static void main(String[] a){
	System.out.println(new Fac().ComputeFac(10));
    }
}

class Fac extends NewFac {
int x;
int[] y;
int x;
NewFac newFac;
    public int ComputeFac(int num){
	int num_aux ;
	boolean str;
	
	y[0] = 6;
	
	//x = newFac.realMethod();
	
	if (num < 1)
	    str = false;
	else 
	    num_aux = num * (newFac.ComputeFac(num-1)) ;
	return num_aux ;
    }
}

class NewFac{
 public int ComputeFac(int num){
	int num_aux ;
	if (num < 1)
	    num_aux = 1 ;
	else 
	    num_aux = num * (this.ComputeFac(num-1)) ;
	return num_aux ;
    }
}

class Woo extends Fac{

}
