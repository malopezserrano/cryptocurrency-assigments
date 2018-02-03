package ConsensusFromTrust;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	private double p_graph;
	private double p_malicious;
	private double p_txDistribution;
	private int numRounds;
	private boolean[] followees;
	private Set<Transaction> pendingTransactions;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    	this.p_graph = p_graph;
    	this.p_malicious = p_malicious;
    	this.p_txDistribution = p_txDistribution;
    	this.numRounds = numRounds;
    	this.pendingTransactions = new HashSet<Transaction>();
    }

    public void setFollowees(boolean[] followees) {
    	this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
    	for(Transaction tx: pendingTransactions) {
            this.pendingTransactions.add(tx);
          }
    }

    public Set<Transaction> sendToFollowers() {
    	return this.pendingTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
    	for (Candidate candidate : candidates) {
    		if (followees[candidate.sender])
    			this.pendingTransactions.add(candidate.tx);
    	}
    }
}
