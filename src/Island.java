package enricoCagnazzo;

import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Island extends Agent {

	private final double muteProb=0.1;
	
	private static final long serialVersionUID = 1L;
	/**viene creata con i seguenti parametri:
	 * 1) num. abitanti per isola
	 * 2) frequenza migrazione
	 * 3) num. migranti
	 * 4) num. iterazioni
	 * 5) AID master agent
	 */
	private int nAbitanti;
	private int freqMigr;
	private int nMigranti;
	private int nIter;
	private ArrayList<Inhabitant> islPop,sons;//island population
	private AID nextIsland, masterAgent;
	
	public void setup(){
		Object[] args = getArguments();
    	nAbitanti	= (int) args[0];
    	freqMigr 	= (int) args[1];
    	nMigranti	= (int) args[2];
    	nIter 	 	= (int) args[3];
    	masterAgent = (AID) args[4];
		//initialize the popolation
    	islPop=new ArrayList<Inhabitant>();
    	for (int i=0;i<nAbitanti;i++)
    		islPop.add(new Inhabitant());
    	sons=new ArrayList<Inhabitant>();
    	//start iterations
    	this.addBehaviour(new Iteration());
    }
	
	
	public class Iteration extends Behaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int iterations=1;
		public void action(){
			final SequentialBehaviour sequence = new SequentialBehaviour(myAgent);
			sequence.addSubBehaviour(new Reproduce());
			sequence.addSubBehaviour(new Mute());
			sequence.addSubBehaviour(new Select());
			if (iterations%freqMigr == 0){
				sequence.addSubBehaviour(new SendMigrants());
				sequence.addSubBehaviour(new ReceiveMigrants());
			}
			this.myAgent.addBehaviour(sequence);
			iterations++;
		}
		
		public boolean done(){
			if (iterations>nIter)
				myAgent.doDelete();
			return iterations>nIter;
		}
	}

	public class Reproduce extends OneShotBehaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action(){
			sons.clear();
			Random r=new Random();
			for (Inhabitant i:islPop)
				sons.add(i.reproduce(islPop.get(r.nextInt(nAbitanti))));
			
		}
		
	}
	
	public class Mute extends OneShotBehaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action(){
			for (Inhabitant i:sons)
				i.mute(muteProb);
		}
	}
	
	public class Select extends OneShotBehaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action(){
			int totale=0;
			Random r=new Random();
			ArrayList<Inhabitant> totalPop=new ArrayList<Inhabitant>(islPop);
			totalPop.addAll(sons);
			islPop.clear();
			for (int i=0;i<nAbitanti;i++){
				ArrayList<Integer>roulette=new ArrayList<Integer>();
				for (Inhabitant in:totalPop){
					totale+=in.getFitness();
					roulette.add(totale);
				}
				int nextIn=r.nextInt(totale);
				//scorro l'array fin quando non arrivo al settore giusto
				for (int j=0;j<roulette.size();j++){
					if (roulette.get(j)>nextIn){
						islPop.add(totalPop.get(j));
						totalPop.remove(j);
						break;
					}
				}
			}
		}
	}
	
	public class SendMigrants extends OneShotBehaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action(){
			if (nextIsland==null){
				//wait to receive the message from the master agent
				MessageTemplate mt=MessageTemplate.MatchSender(masterAgent);
				//voglio che l'agente resti bloccato fin quando non riceve l'AID dell'isola e possa far migrare alcuni individui
				ACLMessage mess=blockingReceive(mt);
				String[] aid=mess.getContent().split(" ");
				nextIsland=new AID();
				nextIsland.setName(aid[3]);
				nextIsland.addAddresses(aid[7]);
			}
			//send nMigranti to the next island
			Random r=new Random();
			for (int i=0;i<nMigranti;i++){
				final ACLMessage mess=new ACLMessage(ACLMessage.INFORM);
				mess.addReceiver(nextIsland);
				mess.setContent(islPop.get(r.nextInt(nAbitanti)).toString());
				this.myAgent.send(mess);
			}
		}
	}
	
	public class ReceiveMigrants extends OneShotBehaviour{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void action(){
			//wait to receive nMigranti from the previous island
			for (int i=0;i<nMigranti;i++){
				MessageTemplate mt=MessageTemplate.not(MessageTemplate.MatchSender(masterAgent));
				ACLMessage mess=blockingReceive(mt);
				Inhabitant migrant=new Inhabitant(mess.getContent());
				integreteMigrant(migrant);
			}
		}
		
		/**
		 * Swap the worst inhabitant with migrant if this is better
		 * @param migrant
		 */
		private void integreteMigrant(Inhabitant migrant) {
			int min=migrant.getFitness(),pos=-1;
			for (int i=0;i<nAbitanti;i++){
				if (islPop.get(i).getFitness()<min){
					min=islPop.get(i).getFitness(); 
					pos=i;
				}
			}
			if (pos!=-1)
				islPop.set(pos, migrant);
		}
	}

	public void takeDown(){
		//send the best inhabitant to the masterAgent
		final ACLMessage mess=new ACLMessage(ACLMessage.INFORM);
		mess.addReceiver(masterAgent);
		mess.setContent(bestInhabitant().toString());
		this.send(mess);
	}

	private Inhabitant bestInhabitant() {
		int max=-1,pos=0;
		for (int i=0;i<nAbitanti;i++){
			if (islPop.get(i).getFitness()>max){
				pos=i;
				max=islPop.get(i).getFitness();
			}
		}
		return islPop.get(pos);
	}
	
}
