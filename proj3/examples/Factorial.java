class Factorial{
  public static void main(String[] a){

    System.out.println(new Fac().ComputeFac(10, 2, 3));
  }
}

class Fac {
  int[] numa;
  public int ComputeFac(int num, int num2, int num3){
    int num_aux;
    int numx;
    int numy;
    int numz;
    numx = 3;
    numz = 5;
    numa = new int[num_aux];
    numa[numy] = numx;
    //numa = new int[num];
    //System.out.println(numa[num_aux]);
    //numb = new Point();
    //while ((num_aux = num)) {
    //  num_aux = num_aux + 1;
    //  numx = numx + 2;
    //  numy = numy + 3;
    //  numz = numz + 4;
    //}
    //num_aux = 5+4*2/7%6-8;
    //if (!(num_aux < numx)) {
    //  num_aux = 1;
    //  numx = numx + 2;
    //  numy = numy + 3;
    //  numz = numz + 4;
    //}
    //else {
    //  num_aux = num * (this.ComputeFac(num-1)) ;
    //  numx = numx + 2;
    //  numy = numy + 3;
    //  numz = numz + 4;
    //}

    //num_aux = 1 ;
    //numx = numx + 2;
    //numy = numy + 3;
    //numz = numz + 4;

    System.out.println(num_aux);
    System.out.println(numx);
    System.out.println(numy);
    System.out.println(numz);
    return num_aux ;
  }
}
