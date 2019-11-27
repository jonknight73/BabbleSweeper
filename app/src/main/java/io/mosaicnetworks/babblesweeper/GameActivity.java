package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import io.mosaicnetworks.babble.node.BabbleService;
import io.mosaicnetworks.babble.node.ServiceObserver;

public class GameActivity extends AppCompatActivity  implements ServiceObserver {

    int COLUMNS = 8;
    int ROWS = 12;
    int numCells = COLUMNS * ROWS;

    int numberOfBombs = 10;


    int MAXPLAYERS = 7;
    int PLAYERS = 0;

    int initialSquares[] = {COLUMNS + 1, numCells - 2 - COLUMNS, (2*COLUMNS)-2, numCells - COLUMNS - COLUMNS + 1};



    boolean showPlayers = false;



    public enum TheCellState {
        BOMB,
        EMPTY,
        THEIRS,
        PENDING,
        MINE,
    }

    class Cell {
        TheCellState CellState = TheCellState.EMPTY;
        int Owner = -1;
        int Neighbours = 0;
        int ID = -1;

        public Cell (int id) {
            this.ID = id;
        }
    }




    class Player {
        String PublicKey;
        int squares = 1;
        String moniker;
        boolean isDead = false;
    }


    Cell TheCells[];
    Player ThePlayers[];

    int MyPlayerIdx = 0;
    boolean isPending = false;


    GameState gameState;

    public enum GameState {
        WAITINGTOSTART,
        PLAYING,
        IAMDEAD,
        GAMEOVER
    }


    private String mMoniker;
    private final MessagingService mMessagingService = MessagingService.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Log.i(MainActivity.TAG, "GameActivity.onCreate");

        InitialiseBoard();


        Intent intent = getIntent();
        mMoniker = intent.getStringExtra("MONIKER");

  //      initialiseAdapter();
        mMessagingService.registerObserver(this);

        if (mMessagingService.getState()!= BabbleService.State.RUNNING_WITH_DISCOVERY) {
            Toast.makeText(this, "Unable to advertise peers", Toast.LENGTH_LONG).show();
        }


        mMessagingService.submitTx(new Message("AP:" + mMoniker + ":" +
                mMessagingService.publicKey(), mMoniker).toBabbleTx());



// Actually start the game play now we have babble up and at them.
        gameState = GameState.WAITINGTOSTART;
        SetStatusMessage("Waiting to Start");

    //    StartGame();
    }



    private void InitialiseBoard() {
        Log.i(MainActivity.TAG, "InitialiseBoard");

        int width_weight = 100 / COLUMNS;
        int height_weight = 80 / ROWS;
        showPlayers = false;
        PLAYERS = 0;
        isPending = false;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cellNo = (int) v.getTag();
                Log.i(MainActivity.TAG, String.format("Clicked %d", cellNo));

// If GameState is not in Playing State, you cannot do anything with the cells
                if (gameState != GameState.PLAYING) {
                    return;
                }

                if (isPending) {return ; }

                switch (TheCells[cellNo].CellState) {
                    case THEIRS:
                        ChangeGameState(GameState.IAMDEAD);
                        mMessagingService.submitTx(new Message("SQ:" + Integer.toString(MyPlayerIdx) + ":" + Integer.toString(cellNo), mMoniker).toBabbleTx());
                        SetStatusMessage("Someone else is here. You lose");
                        return;
                    case BOMB:
                        ChangeGameState(GameState.IAMDEAD);
                        mMessagingService.submitTx(new Message("SQ:" + Integer.toString(MyPlayerIdx) + ":" + Integer.toString(cellNo), mMoniker).toBabbleTx());
                        SetStatusMessage("BOMB! You lose");
                        return;
                    case MINE:
                        return;
                    case PENDING:
                        // Do nothing
                        return;
                }

                // We clicked on an empty square.

                int neighbours[] = NeighbouringCells(cellNo);
                boolean isNeighbour = false;

                for (int i = 0; i < neighbours.length; i++) {
                    if (neighbours[i] > -1) {
                        if ((TheCells[neighbours[i]].CellState == TheCellState.MINE) ||
                                (TheCells[neighbours[i]].CellState == TheCellState.PENDING)) {
                            isNeighbour = true;
                        }
                    }
                }

                if (!isNeighbour) {
                    return;
                }

                TheCells[cellNo].CellState = TheCellState.PENDING;
                isPending = true;
                UpdateNeighbours(cellNo);
                mMessagingService.submitTx(new Message("SQ:" + Integer.toString(MyPlayerIdx) + ":" + Integer.toString(cellNo), mMoniker).toBabbleTx());

            }
        };

        ThePlayers = new Player[MAXPLAYERS];
        TheCells = new Cell[numCells];

        TableLayout tl = (TableLayout) findViewById(R.id.tableLayout);
        TableRow tr = new TableRow(this); //TODO remove this line and suppress the resultant warning

        for (int i = 0; i < numCells; i++) {

//            Log.i(MainActivity.TAG, String.format("Loop Row %2d", i));

            if (i % COLUMNS == 0) {
                tr = new TableRow(this);


                tr.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.FILL_PARENT, height_weight));
                tr.setWeightSum(100);
                tl.addView(tr);
            }

            ImageView ib = new ImageView(this);


            ib.setId(View.generateViewId());


            ib.setTag(i);
            TheCells[i] = new Cell(ib.getId());

            tr.addView(ib);

            ib.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.FILL_PARENT, width_weight));
            ib.setScaleType(ImageView.ScaleType.FIT_XY);
            //         ib.att(style="?android:attr/borderlessButtonStyle");
            int id = getResources().getIdentifier("blank", "drawable", this.getPackageName());

            ib.setImageResource(id);
            ib.setAdjustViewBounds(true);
            ib.setOnClickListener(listener);

        }

    }


    public void sendStartGameMessage(View view){
        //TODO, disable this to prevent duplicate messages. There is a state check so additional messages are ignored



        int targetBombs = 2 * numberOfBombs;
        String bombString = Integer.toString(numberOfBombs);

        for (int i = 0; i < targetBombs; i++) {
            bombString += "¬"+Integer.toString(new Random().nextInt(numCells));
        }

        mMessagingService.submitTx(new Message("SG:"+bombString, mMoniker).toBabbleTx());

       SetStatusMessage("Waiting for the others");
    }


    private void StartGame(String bombString) {

//        ThePlayers = new Player[MAXPLAYERS];

/*
        for (int j = 0; j < ThePlayers.length; j++) {
            ThePlayers[j] = new Player();
        }

*/
        int PlayerCell = -1;
        for (int PlayerCnt = 0 ; PlayerCnt < PLAYERS ; PlayerCnt++){


            int possibleCell = initialSquares[PlayerCnt];

            if (MyPlayerIdx == PlayerCnt) {
                TheCells[possibleCell].CellState = TheCellState.MINE;
                PlayerCell = possibleCell;
            } else {
                TheCells[possibleCell].CellState = TheCellState.THEIRS;
                ChangeCellState((possibleCell)); //TODO remove this line whne players are invisible
            }
            TheCells[possibleCell].Owner = PlayerCnt;

        }


 /*
        int PlayerCnt = 0;
        do {
            int possibleCell = new Random().nextInt(numCells);
            if ( TheCells[possibleCell].CellState == TheCellState.EMPTY ) {
                if (MyPlayerIdx == PlayerCnt) {
                    TheCells[possibleCell].CellState = TheCellState.MINE;
                    PlayerCell = possibleCell;
                } else {
                    TheCells[possibleCell].CellState = TheCellState.THEIRS;
                    ChangeCellState((possibleCell)); //TODO remove this line whne players are invisible
                }
                TheCells[possibleCell].Owner = PlayerCnt;
                PlayerCnt++;            }
        } while (PlayerCnt < PLAYERS );

   */


        // The bomb string is a ¬ delimited list of numbers. The first is the max number of bombs.
        // The rest are random cells. The node starting the game generates them. Once we get max
        // number of bombs we stop adding. If the string runs out before we reach max number, then
        // we make do with what we have.

        Log.i(MainActivity.TAG, bombString);

        String[] arrBombs = bombString.split("¬");

        if (arrBombs.length > 2) {  // If less than 2 params there are no bombs defined
            try {
                numberOfBombs = Integer.parseInt(arrBombs[0]);
            } catch (Exception e) {
                Log.e(MainActivity.TAG, e.getMessage()) ;
                numberOfBombs = 10;
            }
            int bombIdx = 0;
            int bombCnt = 0;

            do {
                int possibleCell = Integer.parseInt(arrBombs[bombIdx]);
                if ( TheCells[possibleCell].CellState == TheCellState.EMPTY ) {
                    TheCells[possibleCell].CellState = TheCellState.BOMB;
                    ChangeCellState(possibleCell);
                    bombCnt++;
                }
                bombIdx++;
            }  while ((bombCnt < numberOfBombs)&&(bombIdx < arrBombs.length )) ;

            numberOfBombs = bombCnt;

        }





// This will need to be deterministic for the multinode approach

        ChangeGameState(GameState.PLAYING);
        // Defer so all the other players are in situ
        UpdateNeighbours(PlayerCell);


        SetStatusMessage("Game On...");

    }



    private void ChangeGameState(GameState newState){
        if (newState == gameState) { return ; } //No change do nothing

        gameState = newState;

        if ( ( (newState == GameState.IAMDEAD) || (newState == GameState.GAMEOVER)  ) && ( ! showPlayers ) ) {
            showPlayers = true;

            for (int i = 0; i < numCells ; i++){
  //              if ( (TheCells[i].CellState == TheCellState.THEIRS) || (TheCells[i].CellState == TheCellState.BOMB) ) {
                    ChangeCellState(i);
  //              }
            }


        }

    }



    private void SetStatusMessage(String msg) {
        TextView tv= (TextView) findViewById(R.id.statusText);
        tv.setText(msg);
    }


    private void SetScoreMessage() {

        String myScore = "";
        String theirScore = "";
        for (int i = 0; i < ThePlayers.length; i++ ) {
            if ( i == MyPlayerIdx) {
                myScore = Integer.toString(ThePlayers[i].squares);
            } else {
                theirScore += " : "+Integer.toString(ThePlayers[i].squares);
            }
        }



        TextView tv= (TextView) findViewById(R.id.scoreText);
        tv.setText(myScore + " " + theirScore);
    }



    private int[] NeighbouringCells(int cellNo) {
        int neighbours[] = new int[9];

// Top 3
        if (cellNo >= COLUMNS) {
            if (cellNo%COLUMNS > 0) { //TopLeft
                neighbours[0] = cellNo - COLUMNS - 1;
            } else {
                neighbours[0] = -1;
            }

            neighbours[1] = cellNo - COLUMNS; //TopCentre

            if (cellNo%COLUMNS < (COLUMNS - 1) ) { //TopRight
                neighbours[2] = cellNo - COLUMNS + 1;
            } else {
                neighbours[2] = -1;
            }



        } else {
            neighbours[0] = -1;
            neighbours[1] = -1;
            neighbours[2] = -1;
        }



        if (cellNo%COLUMNS > 0) { //MiddleLeft
            neighbours[3] = cellNo  - 1;
        } else {
            neighbours[3] = -1;
        }

        neighbours[4] = cellNo;


        if (cellNo%COLUMNS < (COLUMNS - 1) ) { //MiddleRight
            neighbours[5] = cellNo  + 1;
        } else {
            neighbours[5] = -1;
        }



// Bottom 3
        if (cellNo < (numCells - COLUMNS)) {
            if (cellNo%COLUMNS > 0) { //BottomLeft
                neighbours[6] = cellNo + COLUMNS - 1;
            } else {
                neighbours[6] = -1;
            }

            neighbours[7] = cellNo + COLUMNS; //BottomCentre

            if (cellNo%COLUMNS < (COLUMNS - 1) ) { //BottomRight
                neighbours[8] = cellNo + COLUMNS + 1;
            } else {
                neighbours[8] = -1;
            }



        } else {
            neighbours[5] = -1;
            neighbours[6] = -1;
            neighbours[7] = -1;
        }

        return neighbours;
    }





    private void UpdateNeighboursNeighbours(int cellNo) {
        int neighbours[] = NeighbouringCells(cellNo);

        for (int j = 0; j< neighbours.length; j++) {
            if (neighbours[j] >= 0) {
                UpdateNeighbours(neighbours[j]);
            }
        }
    }



    private void UpdateNeighbours(int cellNo) {
        // ROWS, COLUMNS
                int neighbourCount = 0;
                int neighbours[] = NeighbouringCells(cellNo);
                for (int j = 0; j< neighbours.length; j++) {
                    if (neighbours[j] >= 0) {
                        Cell thisCell = TheCells[neighbours[j]];

                        if ( (thisCell.CellState == TheCellState.BOMB) ||
                                (thisCell.CellState == TheCellState.THEIRS)) {
                            neighbourCount++;
                        }

                    }
                }
                TheCells[cellNo].Neighbours = neighbourCount;
                ChangeCellState(cellNo);
    }



    private void ChangeCellState(int CellNo) {

        String imageName = "";

        ImageView ib = (ImageView) findViewById(TheCells[CellNo].ID);

        switch(TheCells[CellNo].CellState) {
            case PENDING:
                if ((gameState == GameState.IAMDEAD)||(gameState == GameState.GAMEOVER)) {
                    imageName = "person" + Integer.toString(MyPlayerIdx);
                } else {
                    imageName = "pending";
                 //   int neighbours = TheCells[CellNo].Neighbours;
                 //   imageName = "cell" + Integer.toString(neighbours);
                }
                break;
            case EMPTY:
                imageName = "blank";
                break;
            case BOMB:
                if (showPlayers) {
                    imageName = "bomb";
                } else {
                    imageName = "blank";
                }

                break;
            case THEIRS:
                if (showPlayers) {
                    //  imageName = "person";  //TODO set to blank to make invisible
                     imageName = "person" + Integer.toString(TheCells[CellNo].Owner);
                     Log.i(MainActivity.TAG, imageName);

                } else {
                    imageName = "blank";
                }
                break;
            default:   // MINE
                if ((gameState == GameState.IAMDEAD)||(gameState == GameState.GAMEOVER)) {
                    imageName = "person" + Integer.toString(MyPlayerIdx);
                    Log.i(MainActivity.TAG, imageName);
                } else {
                    int neighbours = TheCells[CellNo].Neighbours;
                    imageName = "cell" + Integer.toString(neighbours);
                }
        }


        int id = getResources().getIdentifier(imageName, "drawable", ib.getContext().getPackageName());
        ib.setImageResource(id);

    }

    @Override
    public void stateUpdated() {
        final Message message = mMessagingService.state.getLatestMessage();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProcessMessage(message);
            }
        });



    }

 /*
    Colon separated string - its good enough for this complexity

 Parameter 1:
    MsgType
        AG : Announce Game
        AP : Announce Player
        SQ : Claim Square
        OM : Out of Moves
        SG : Start Game

   Each of these are then documented separately:

   SQ
   ==

   Parameter 1: SQ
   Parameter 2: Player No
   Parameter 3: Cell No

 */

    public void ProcessMessage(Message message) {

        String msgText = message.getText();
        Log.i(MainActivity.TAG, msgText);

        String[] arrMessage = msgText.split(":", 3);

        if (arrMessage.length > 0){

            switch (arrMessage[0]) {
                case "SQ":   // Claiming a Square
                      if (arrMessage.length == 3) {   // Must be 3 to be well formed
                          int playerNo = Integer.parseInt(arrMessage[1]);
                          int cellNo = Integer.parseInt(arrMessage[2]);

                          if (playerNo == MyPlayerIdx){
                              processPendingCellMessage(cellNo);
                          } else {
                              processTheirCellMessage(playerNo, cellNo);
                          }
                      }

                    break;
                case "SG":
                    if (arrMessage.length == 2) { // Must be 3 to be well formed

                        if (gameState == GameState.WAITINGTOSTART) {
                            StartGame(arrMessage[1]);
                        }
                    }
                    break;

                case "AP":  // Add Player
                    if (arrMessage.length == 3) {   // Must be 3 to be well formed
                        Player player = new Player();
                        player.PublicKey = arrMessage[2];
                        player.moniker = arrMessage[1];

                        if ( PLAYERS >= MAXPLAYERS) {
                            break;
                        }

                        for (int i = 0; i < PLAYERS; i++) {
                            if (player.PublicKey.equals(ThePlayers[i].PublicKey)) { return ;} // no duplicates
                        }


                        ThePlayers[PLAYERS] = player;
                        PLAYERS++;

                        if ( player.PublicKey.equals(mMessagingService.publicKey()) ) {
                            MyPlayerIdx = PLAYERS - 1;
                            SetStatusMessage("You have joined, "+player.moniker);
                        } else {
                            Log.i(MainActivity.TAG, player.PublicKey);
                            Log.i(MainActivity.TAG, mMessagingService.publicKey());

                            SetStatusMessage(player.moniker + " has joined. We have " + Integer.toString(PLAYERS) + " players.");
                        }
                    }

//                mMessagingService.submitTx(new Message("AP:" + mMoniker + ":" +
  //                      mMessagingService.publicKey(), mMoniker).toBabbleTx());

                    break;
                default:
                    Log.e(MainActivity.TAG, "Unknown Babble Message: "+msgText);

            }

        }

    }


    public void processPendingCellMessage(int cellNo){
        if (ThePlayers[MyPlayerIdx].isDead) {return;}
        switch (TheCells[cellNo].CellState) {
            case PENDING:   //Alls Fine
                TheCells[cellNo].CellState = TheCellState.MINE;
                ThePlayers[MyPlayerIdx].squares++;
                isPending = false;
                ChangeCellState(cellNo);
                SetScoreMessage();
            break;
            case THEIRS:    // We dead
                ChangeGameState(GameState.IAMDEAD);
                SetStatusMessage("Someone else is here. You lose");
            break;
            default:
                // This has gone wrong

        }



    }


    public void processTheirCellMessage(int playerNo, int cellNo){

        if (ThePlayers[playerNo].isDead) { return ;}
        switch (TheCells[cellNo].CellState) {
            case THEIRS:
                if (TheCells[cellNo].Owner == playerNo) {return;}
                // NB deliberately no break statement
            case MINE:
            case BOMB:
                ThePlayers[playerNo].isDead = true;
                break;
            case PENDING:
                // Do nothing - when it reached consensus, you will die
            case EMPTY:
                TheCells[cellNo].CellState = TheCellState.THEIRS;
                TheCells[cellNo].Owner = playerNo;
                ThePlayers[playerNo].squares++;
                UpdateNeighboursNeighbours(cellNo);
                SetScoreMessage();
                break;
        }
    }




    @Override
    public void onBackPressed() {
        mMessagingService.leave(null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        mMessagingService.removeObserver(this);

        super.onDestroy();
    }


}
