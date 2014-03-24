class BubbleSort{
    public static void main(String[] a){
	System.out.println(10);//new Fac().ComputeFac(10));
    }
}


// This class contains the array of integers and
// methods to initialize, print and sort the array
// using Bublesort
class Fac{

    public int ComputeFac(int num){
	int num_aux ;
	boolean str;

	if(true)
	    str = false;
	if (num < 1)
	    num_aux = 1;
	else 
	    num_aux = num * (this.ComputeFac(num-1)) ;

	return num_aux ;
    }
}

   



