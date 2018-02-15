package enricoCagnazzo;

import java.util.ArrayList;
import java.util.Date;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class MasterAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**viene creato con i seguenti parametri:
	 * 1) num. isole
	 * 2) num. abitanti per isola
	 * 3) frequenza migrazione
	 * 4) num. migranti
	 * 5) num. iterazioni
	 */
	int nIsole;
	int nAbitanti;
	int freqMigr;
	int nMigranti;
	int nIter;
	long start,end; //to calculate the lenght of the elaboration

	ArrayList<Inhabitant> bestInhabitants=new ArrayList<Inhabitant>();
	
	@Override
	public void setup(){
		start=new Date().getTime();
		Object[] args = getArguments();
		if (args.length!=5){
			System.err.println("Wrong number of arguments, must be 5: nIsole,nAbitanti,freqMigr,nMigranti,nIter");
			this.doDelete();
			return;
		}
    	nIsole   = Integer.parseInt(args[0].toString());
    	nAbitanti= Integer.parseInt(args[1].toString());
    	freqMigr = Integer.parseInt(args[2].toString());
    	nMigranti= Integer.parseInt(args[3].toString());
    	nIter 	 = Integer.parseInt(args[4].toString());
    	if (nIsole==1) //se ho una sola isola evito che vengano effettuate migrazioni 
    		freqMigr=nIter+1;
		//invoca Creator
		this.addBehaviour(new Creator());
		//invoca Finish
		this.addBehaviour(new Finish());
	}
	
	
	public class Creator extends OneShotBehaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void action(){
			ContainerController cc=myAgent.getContainerController(); //mi serve per sapere dove creare le isole
			long time=new Date().getTime(); //mi serve per creare dei nomi "unici" per le isole
			// crea le n isole
			for (int i=0;i<nIsole;i++){
				 Object[] args ={nAbitanti,freqMigr,nMigranti,nIter,this.myAgent.getAID()};				 
				 try {
					 cc.createNewAgent(time+"Island"+i, "enricoCagnazzo.Island", args).start();
				} catch (StaleProxyException e) {
					e.printStackTrace();
				}
			}
			//invia ad ogni isola la "successiva" alla quale dovrà inviare i migranti
			for (int i=0;i<nIsole-1;i++){
				final ACLMessage mess=new ACLMessage(ACLMessage.INFORM);
				mess.addReceiver(this.myAgent.getAID(time+"Island"+i));
				mess.setContent(this.myAgent.getAID(time+"Island"+(i+1)).toString());
				this.myAgent.send(mess);
			}
			final ACLMessage mess=new ACLMessage(ACLMessage.INFORM);
			mess.addReceiver(this.myAgent.getAID(time+"Island"+(nIsole-1)));
			mess.setContent(this.myAgent.getAID(time+"Island"+0).toString());
			this.myAgent.send(mess);
		}
	}
	
	public class Finish extends Behaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public void action(){
			//waiting for receive the islands' results
			ACLMessage mess=blockingReceive();
			bestInhabitants.add(new Inhabitant(mess.getContent()));
		}

		@Override
		public boolean done() {
			if (bestInhabitants.size()==nIsole)
				myAgent.doDelete();
			return bestInhabitants.size()==nIsole;
		}
		
	}
	
	public void takeDown(){
		//trova la soluzione migliore
		if (bestInhabitants.size()==0) return;
		Inhabitant best=bestInhabitants.get(0);
		for (int i=1;i<nIsole;i++){
			if (bestInhabitants.get(i).getFitness()>best.getFitness())
				best=bestInhabitants.get(i);
		}
		end=new Date().getTime();
		//mostra il risultato
		System.out.println("Best value: "+best.getFitness()+'\n'+"Global time: "+((end-start))+" ms");
	}
	
}
