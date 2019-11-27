package io.mosaicnetworks.babblesweeper;

import android.util.Log;

import io.mosaicnetworks.babble.node.BabbleService;

public final class MessagingService extends BabbleService<AppState> {

    private static MessagingService INSTANCE;

    private static String MyPublicKey = Double.toString(Math.random());

    public static String publicKey() {
        return MyPublicKey;
        //TODO this is a hack. When publicKey is avbailable in BabbleService, the method should be
        // inherited and we can lose the random number.
    }


    // Wraps submitTX and logs every transaction...
        public void submitTx(BabbleTx tx) {
            Log.i(MainActivity.TAG, tx.text);
            super.submitTx(tx);
        }



        public static MessagingService getInstance() {
        if (INSTANCE==null) {
            INSTANCE = new MessagingService();
        }

        return INSTANCE;
    }



    private MessagingService() {
        super(new AppState());
    }
}

