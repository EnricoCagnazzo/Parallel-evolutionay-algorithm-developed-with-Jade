package enricoCagnazzo;

import java.util.ArrayList;
import java.util.Random;

public class Inhabitant {

	//strutt. dati soluzione
	private ArrayList<Boolean> sol;
	private int dim=100;
	//fitness
	private int fitness;
	
	//private Random rand=new Random();
	
	public Inhabitant(){
		sol=new ArrayList<Boolean>();
		Random rand=new Random();
		for  (int i=0;i<dim;i++)
			sol.add(rand.nextBoolean());
		this.setFitness();
	}
	
	public Inhabitant(String s){
		sol=new ArrayList<Boolean>();
		for (int i=0;i<dim;i++)
			sol.add(s.charAt(i)=='1'?true:false);
		this.setFitness();
	}
	
	public Inhabitant(ArrayList<Boolean> s){
		sol=new ArrayList<Boolean>(s);
		this.setFitness();
	}
	
	public void setFitness(){
		this.fitness=0;
		for (Boolean b:sol)
			if (b) this.fitness++;
	}
	
	public int getFitness(){
		return fitness;
	}
	
	public ArrayList<Boolean> getSol(){
		return sol;
	}
	
	//riproduzione
	public Inhabitant reproduce(Inhabitant parent2){
		Random rand=new Random();
		int a=rand.nextInt(dim), b=rand.nextInt(dim), cob=a<b?a:b, coe=a>b?a:b; //cob=CrossOver Begin, coe=CrossOver End
		ArrayList<Boolean> son=new ArrayList<Boolean>();
		for (int i=0;i<dim;i++){
			son.add((i<cob||i>coe)?this.sol.get(i):parent2.getSol().get(i));
		}
		//		System.arraycopy(this.sol		 , 0	, son, 0	, cob	 );
//		System.arraycopy(parent2.getSol(), cob-1, son, cob-1, coe-cob);
//		System.arraycopy(this.sol		 , coe	, son, coe	, dim-coe);
		return new Inhabitant(son);
	}
	
	//mutazione
	public void mute(double muteProb){
		Random rand=new Random();
		if (rand.nextDouble()<muteProb){
			int p=rand.nextInt(dim);
			sol.set(p, !sol.get(p));
			this.setFitness();
		}
	}
	
	public String toString(){
		String s="";
		for (int i=0;i<dim;i++)
			s=s.concat(sol.get(i)?"1":"0");
		return s;
	}
	
}
