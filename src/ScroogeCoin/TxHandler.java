package ScroogeCoin;
//https://www.coursera.org/learn/cryptocurrency/programming/KOo3V/scrooge-coin

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class TxHandler {
    private UTXOPool pool;
    private TxValidator validator = new TxValidator();

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.pool = new UTXOPool(utxoPool);
    }
    
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        return validator.validate(this.pool, tx);
    }

    private class TxValidator {
        private double txInSum = 0, txOutSum = 0;

        public boolean validate(UTXOPool pool, Transaction tx) {
            return txNotNull(tx) && txInputValid(pool, tx) && txOutputValid(tx);
        }

        private boolean txNotNull(Transaction tx) {
            return tx != null;
        }
        
        /**
         * @return true if:
         * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
         * (2) the signatures on each input of {@code tx} are valid, 
         * (3) no UTXO is claimed multiple times by {@code tx}
         */
        private boolean txInputValid(UTXOPool pool, Transaction tx) {
            txInSum = 0;
            Set<UTXO> usedTxs = new HashSet<UTXO>();
            
            for (int i = 0; i < tx.numInputs(); i++) {
                Transaction.Input input = tx.getInput(i);
                if (input == null) {
                    return false;
                }
                
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                //Rule 1
                if (!pool.contains(utxo)) {
                    return false;
                }
                Transaction.Output prevTxOut = pool.getTxOutput(utxo);

                //Rule 2
                PublicKey pubKey = prevTxOut.address;
                byte[] message = tx.getRawDataToSign(i);
                byte[] signature = input.signature;
                if (!Crypto.verifySignature(pubKey, message, signature)) {
                    return false;
                }

                //Rule 3
                if (usedTxs.contains(utxo)) {
                	return false;
                }
                usedTxs.add(utxo);

                txInSum += prevTxOut.value;
            }
            return true;
        }
        
        /**
         * @return true if:
         * (4) a {@code tx}s output value is non-negative
         * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
         *     values; and false otherwise
         */
        private boolean txOutputValid(Transaction tx) {
            txOutSum = 0;
            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output txOut = tx.getOutput(i);
                //Rule 4
                if (txOut.value < 0) {
                    return false;
                }
                txOutSum += txOut.value;
            }
            //Rule 5
            return txInSum >= txOutSum;
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null) {
            return new Transaction[0];
        }
        ArrayList<Transaction> valid = new ArrayList<Transaction>();
        int txCount;
            for (Transaction tx : possibleTxs) {
                if (!isValidTx(tx)) {
                    continue;
                }
                valid.add(tx);
                this.applyTx(tx);
            }
        return valid.toArray(new Transaction[valid.size()]);
    }

    private void applyTx(Transaction tx) {
        if (tx == null) {
            return;
        }
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            this.pool.removeUTXO(utxo);
        }
        byte[] txHash = tx.getHash();
        int index = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            UTXO utxo = new UTXO(txHash, index);
            index += 1;
            this.pool.addUTXO(utxo, output);
        }
    }
}