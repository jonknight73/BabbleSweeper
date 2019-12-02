package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.security.spec.ECField;
import java.util.Random;

import io.mosaicnetworks.babble.node.BabbleService;
import io.mosaicnetworks.babble.node.ServiceObserver;

public class GameActivity extends AppCompatActivity  implements ServiceObserver {


// Hardcoded grid size and number of bombs
//TODO - set these parameters in the Start Game message and make the game dynamically configurable
    int COLUMNS = 8;
    int ROWS = 12;
    int numCells = COLUMNS * ROWS;
    int numberOfBombs = 16;


    int MAXPLAYERS = 8;
    int PLAYERS = 0;


    // Initial squares
    // The player order is agreed by the consensus engine, so using a deterministic list make sense.
    int[] initialSquares = { new Point(1,1).ThisCellNo(),
                             new Point(COLUMNS-2,ROWS-2).ThisCellNo(),
                             new Point(1,ROWS-2).ThisCellNo(),
                             new Point(COLUMNS-2,1).ThisCellNo(),
                             new Point(3,4).ThisCellNo(),
                             new Point(COLUMNS-3,ROWS-4).ThisCellNo(),
                             new Point(3,ROWS-4).ThisCellNo(),
                             new Point(COLUMNS-3,4).ThisCellNo()
    };

     //       {COLUMNS + 1, numCells - 2 - COLUMNS, (2*COLUMNS)-2, numCells - COLUMNS - COLUMNS + 1};


    // Flag controls whether the screen displays the location of bombs and other players.
    boolean showPlayers = false;



    // State of each square
    public enum TheCellState {
        BOMB,
        EMPTY,
        THEIRS,
        PENDING,
        MINE,
    }


    // Internal Class to hold cell state
    class Cell {
        TheCellState CellState = TheCellState.EMPTY;
        int Owner = -1;
        int Neighbours = 0;
        int ID = -1;

        public Cell (int id) {
            this.ID = id;
        }
    }


    // Internal class to hold player state
    class Player {
        String PublicKey;
        int squares = 1;
        String moniker;
        boolean isDead = false;
    }


    // Internal class for co-ordinates
    // Encaspsulates neighbouring cell algorithm, and conversation to and from Cartesian co-ords
    class Point {
        int x;
        int y;

        Point(int nx, int ny) {
            this.x = nx;
            this.y = ny;
        }


        Point(int Cellno) {
            this.x = Cellno%COLUMNS;
            this.y = (Cellno - this.x)/COLUMNS;
        }

        int ThisCellNo() {
            return CellNo(this.x,this.y);
        }

        int CellNo(int ax, int ay){
            return (ay*COLUMNS)+ax;
        }


        // Generates an array of Cell numbers denoting the neighbouring cells. N.B. included self.
        int[] Neighbours() {
            int[] rtn = new int[9];

/*
   0 1 2
   3 4 5
   6 7 8
*/

// Calculate all neighbours ignoring error conditions

            rtn[4] = this.ThisCellNo();
            rtn[3] = rtn[4] - 1;
            rtn[5] = rtn[4] + 1;
            rtn[0] = rtn[3] - COLUMNS;
            rtn[1] = rtn[4] - COLUMNS;
            rtn[2] = rtn[5] - COLUMNS;
            rtn[6] = rtn[3] + COLUMNS;
            rtn[7] = rtn[4] + COLUMNS;
            rtn[8] = rtn[5] + COLUMNS;


// Now wipe the edge cases
            if (this.y<1) { // TOP ROW
               rtn[0] = -1;
               rtn[1] = -1;
               rtn[2] = -1;
            }

            if (this.y>=(ROWS-1)) { // BOTTOM ROW
                rtn[6] = -1;
                rtn[7] = -1;
                rtn[8] = -1;
            }

            if (this.x<1) { // LEFT COLUMN
                rtn[0] = -1;
                rtn[3] = -1;
                rtn[6] = -1;
            }

            if (this.x>=(COLUMNS-1)) { // RIGHT COLUMN
                rtn[2] = -1;
                rtn[5] = -1;
                rtn[8] = -1;
            }
            return rtn;
        }
    }


    // Arrays to hold Player and Cells objects
    Cell TheCells[];
    Player ThePlayers[];

    int MyPlayerIdx = 0;
    boolean isPending = false;


    // As it sounds the Game state
    GameState gameState;


    // enumerated type for the Game State
    public enum GameState {
        WAITINGTOSTART,
        PLAYING,
        IAMDEAD,
        GAMEOVER
    }


    // This player's moniker - passed in from JoinNetwork / NewNetwork activity
    private String mMoniker;


    // MessagingService instance
    private final MessagingService mMessagingService = MessagingService.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Log.i(MainActivity.TAG, "GameActivity.onCreate");

        // Sets up screen
        InitialiseBoard();


        Intent intent = getIntent();
        mMoniker = intent.getStringExtra("MONIKER");

  //    Configure this activity to receive messages from Babble
        mMessagingService.registerObserver(this);

        if (mMessagingService.getState()!= BabbleService.State.RUNNING_WITH_DISCOVERY) {
            Toast.makeText(this, "Unable to advertise peers", Toast.LENGTH_LONG).show();
        }


        // Announce this player with an AP (Add Player) message
        mMessagingService.submitTx(new Message("AP:" + mMoniker + ":" +
                mMessagingService.publicKey(), mMoniker).toBabbleTx());


        // Set the initial game state
        gameState = GameState.WAITINGTOSTART;
        SetStatusMessage("Waiting to Start");

    }


// This function initialises the game state.
    private void InitialiseBoard() {
        Log.i(MainActivity.TAG, "InitialiseBoard");

        // Calculate layout weights for the cells
        int width_weight = 100 / COLUMNS;
        int height_weight = 80 / ROWS;

        // Make the other players and bombs invisible by default
        showPlayers = false;

        // We have no players - all players are added via "Add Player" messages
        PLAYERS = 0;

        // isPending is set when we click a square, and is unset when we receive a message
        // assigning a square to us
        isPending = false;


        // This is the onclick listener for all squares.
        // We set the tag of each image button to its corresponding cellNo.
        // This allows us to just look at the tag for the clicked button and derive the cell number

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cellNo = (int) v.getTag();
                Log.i(MainActivity.TAG, String.format("Clicked %d", cellNo));

                // If GameState is not in Playing State, you cannot do anything with the cells
                if (gameState != GameState.PLAYING) {
                    return;
                }

                // If you have a pending square, you cannot claim another one
                if (isPending) {return ; }

                // Check the state of the clicked cell
                switch (TheCells[cellNo].CellState) {
                    case THEIRS:     // If it is someone else's square. You die.
                        ChangeGameState(GameState.IAMDEAD);
                        ThePlayers[MyPlayerIdx].isDead = true;
                        mMessagingService.submitTx(new Message("SQ:" + Integer.toString(MyPlayerIdx) + ":" + Integer.toString(cellNo), mMoniker).toBabbleTx());
                        SetStatusMessage("Someone else is here. You lose");
                        CheckIfGameOver();
                        return;
                    case BOMB:       // If is a bomb, you die
                        ChangeGameState(GameState.IAMDEAD);
                        ThePlayers[MyPlayerIdx].isDead = true;
                        mMessagingService.submitTx(new Message("SQ:" + Integer.toString(MyPlayerIdx) + ":" + Integer.toString(cellNo), mMoniker).toBabbleTx());
                        SetStatusMessage("BOMB! You lose");
                        CheckIfGameOver();
                        return;
                    case MINE:      // If you already own it, do nothing
                        return;
                    case PENDING:   // If it is already pending, do nothing
                        // Do nothing
                        return;
                }

                // We clicked on an empty square.

                // Check to see if this square neighbours one of our squares.
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

                if (!isNeighbour) {  // Not neighbouring, so cannot claim this square.
                    return;
                }

                // Update the clicked cell state to Pending
                TheCells[cellNo].CellState = TheCellState.PENDING;
                // Set isPending to prevent claiming another square before this one is decided.
                isPending = true;
                // Update this cell display and its surrounding cells.
                UpdateNeighbours(cellNo);

                // Send Babble message "claiming" the square
                mMessagingService.submitTx(new Message("SQ:" + Integer.toString(MyPlayerIdx) + ":" + Integer.toString(cellNo), mMoniker).toBabbleTx());

            }
        };



        // Initialise the Players anc Cells array
        ThePlayers = new Player[MAXPLAYERS];
        TheCells = new Cell[numCells];

        // The TableLayout is defined empty in the layout XML
        TableLayout tl = (TableLayout) findViewById(R.id.tableLayout);

        TableRow tr = new TableRow(this); //TODO remove this line and suppress the resultant warning


        // THis loop creates TableRows ands within them ImageViews. These are the squares on the game grid
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

        // The number of bombs and a list of random cell numbers, separated by ¬ is sent as part of
        // the Start Game message. This allows all the noeds to have the same state.
        // It is more efficient to just generate a longer string of numbers than required than
        // to do implement logic here.
        // There is potential concurrency issues here as the number of players is not definitively
        // known until the SG message reaches consensus
        int targetBombs = 2 * numberOfBombs;
        String bombString = Integer.toString(numberOfBombs);


        //TODO remove this testing line which boxes in the first player with bombs:
        // bombString="12¬0¬1¬2¬3¬8¬11¬16¬19¬24¬25¬26¬27";


        for (int i = 0; i < targetBombs; i++) {
            bombString += "¬"+Integer.toString(new Random().nextInt(numCells));
        }

        // Send the Start Game message. Any node can send it, but only the first message has any effect.
        mMessagingService.submitTx(new Message("SG:"+bombString, mMoniker).toBabbleTx());

        SetStatusMessage("Waiting for the others");
    }


    private void StartGame(String bombString) {

        // Prepare the game state


        // Place the players. The starting positions of the players are fixed.
        int PlayerCell = -1;
        for (int PlayerCnt = 0 ; PlayerCnt < PLAYERS ; PlayerCnt++){


            int possibleCell = initialSquares[PlayerCnt];

            if (MyPlayerIdx == PlayerCnt) {
                TheCells[possibleCell].CellState = TheCellState.MINE;
                PlayerCell = possibleCell;
            } else {
                TheCells[possibleCell].CellState = TheCellState.THEIRS;
                ChangeCellState((possibleCell));
            }
            TheCells[possibleCell].Owner = PlayerCnt;
        }



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
            int bombIdx = 1;
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


        // Set the onscreen score
        SetScoreMessage();

        ChangeGameState(GameState.PLAYING);
        // Defer so all the other players are in situ

        // Grey out the Start image button - we have already started so should not press it again
        ImageButton buttStart= (ImageButton) findViewById(R.id.btNew);
      //  buttStart.setColorFilter(Color.argb(127, 127,127,127));
        buttStart.setImageAlpha(50);
        buttStart.setOnClickListener(null);

        // Update screen display of our cell, so we can see our starting position.
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


// Show message in the status bar at the bottom of the game screen
    private void SetStatusMessage(String msg) {
        TextView tv= (TextView) findViewById(R.id.statusText);
        tv.setText(msg);
    }



// Show the number of cells claimed by each player. This player has their score first
    private void SetScoreMessage() {

        String myScore = "";
        String theirScore = "";
        for (int i = 0; i < PLAYERS; i++ ) {
           try {
               if (i == MyPlayerIdx) {
                   myScore = Integer.toString(ThePlayers[i].squares);
               } else {
                   theirScore += " : " + Integer.toString(ThePlayers[i].squares);
               }
           } catch (Exception e) { Log.e(MainActivity.TAG, e.getMessage()) ;}
        }



        TextView tv= (TextView) findViewById(R.id.scoreText);
        tv.setText(myScore + " " + theirScore);
    }



//    Generate array of neighbouring cell numbers
    private int[] NeighbouringCells(int cellNo) {
        return new Point(cellNo).Neighbours();
    }


//  Update the neighbours of teh neighbours of the current cell
    private void UpdateNeighboursNeighbours(int cellNo) {
        int neighbours[] = NeighbouringCells(cellNo);

        for (int j = 0; j< neighbours.length; j++) {
            if (neighbours[j] >= 0) {
                UpdateNeighbours(neighbours[j]);
            }
        }
    }


    // Sets the neighbours property of a cell and updates the display for that cell.
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


// This functions sets the screen display for a cell.
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



    // This function receives messages from Babble, then processes them in the UI thread.
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


 // THis function receives messgaes from Babble and processes them.
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
                    if (arrMessage.length == 2) { // Must be 2 to be well formed

                        if (gameState == GameState.WAITINGTOSTART) {
                            StartGame(arrMessage[1]);
                        }
                    }
                    break;
                case "OM":
                    if (arrMessage.length == 2) { // Must be 2 to be well formed

                        int playerNo = Integer.parseInt(arrMessage[1]);
                        if ( ThePlayers[playerNo].isDead ) { return ; } // Ignore duplicates
                        ThePlayers[playerNo].isDead = true;
                        CheckIfGameOver();
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
                CheckIfHasValidMove();
            break;
            case THEIRS:    // We dead

                ChangeGameState(GameState.IAMDEAD);
                ThePlayers[MyPlayerIdx].isDead = true;
                SetStatusMessage("Someone else is here. You lose");
                CheckIfGameOver();
            break;
            default:
                // This has gone wrong

        }
    }



    // This function checks for valid moves for another player.
    // It is useful as it allows us to make their game over.
    public void CheckIfTheyHaveValidMove(int playerNo) {
        if (ThePlayers[playerNo].isDead) { return ;} // Already dead

        int checked[]  = new int[numCells];

        for (int n = 0 ; n < numCells; n++) {
            if ( TheCells[n].CellState != TheCellState.EMPTY ) {continue ; } // Find Empty Cell
            int[] neighbours = new Point(n).Neighbours();
            for (int j = 0 ; j < 9; j++) {
                if (j==4) { continue;} // neighbours include self

                if (neighbours[j] > -1) {
                    if ( checked[neighbours[j]]==0) { // New unchecked square
                        if ((TheCells[neighbours[j]].CellState == TheCellState.THEIRS ) &&
                                (TheCells[neighbours[j]].Owner == playerNo )) {
                            return ; // Found at least one valid move, so exit
                        }
                        checked[neighbours[j]]=1; // Marked as done so we don't check this square again.
                    }
                }
            }
        }


        // No moves found.

        ThePlayers[playerNo].isDead = true;
        mMessagingService.submitTx(new Message("OM:" + Integer.toString(MyPlayerIdx), mMoniker).toBabbleTx()); // Everyone will send one
        CheckIfGameOver();
    }





    // Check if you have any valid moves.
    public void CheckIfHasValidMove() {
        int checked[]  = new int[numCells];



        for (int n = 0 ; n < numCells; n++) {
            if ( TheCells[n].CellState != TheCellState.EMPTY ) {continue ; } // Find Empty Cell
            int[] neighbours = new Point(n).Neighbours();
            for (int j = 0 ; j < 9; j++) {
                if (j==4) { continue;} // neighbours include self

                if (neighbours[j] > -1) {
                    if ( checked[neighbours[j]]==0) { // New unchecked square
                        if ((TheCells[neighbours[j]].CellState == TheCellState.MINE ) ||
                                (TheCells[neighbours[j]].CellState == TheCellState.PENDING )) {
                            return ; // Found at least one valid move, so exit
                        }
                        checked[neighbours[j]]=1; // Marked as done so we don't check this square again.
                    }
                }
            }
        }


        // No moves found.

        ChangeGameState( GameState.IAMDEAD );
        ThePlayers[MyPlayerIdx].isDead = true;
        mMessagingService.submitTx(new Message("OM:" + Integer.toString(MyPlayerIdx), mMoniker).toBabbleTx());
        SetStatusMessage("Out of moves.");
        CheckIfGameOver();

    }




    // Check if all players have finished
    public void CheckIfGameOver() {

        boolean aPlayerIsNotDead = false;
        int biggest = 0;
        String biggestName = "";

        for (int i=0;i<PLAYERS;i++) {
            if (! ThePlayers[i].isDead) {
                aPlayerIsNotDead = true;
            }
            if (ThePlayers[i].squares > biggest) {
                biggest = ThePlayers[i].squares;
                biggestName = ThePlayers[i].moniker;
            } else {
                if  (ThePlayers[i].squares == biggest) {
                    biggestName += ", " + ThePlayers[i].moniker;
                }
            }

        }

        if (aPlayerIsNotDead) { return ; }

        ChangeGameState(GameState.GAMEOVER);
        SetStatusMessage("Game Over, " + biggestName + " wins");

    }



    // Function to process other platyers "SQ" messages
    public void processTheirCellMessage(int playerNo, int cellNo){

        if (ThePlayers[playerNo].isDead) { return ;}
        switch (TheCells[cellNo].CellState) {
            case THEIRS:
                if (TheCells[cellNo].Owner == playerNo) {return;}
                // NB deliberately no break statement
            case MINE:
            case BOMB:
                ThePlayers[playerNo].isDead = true;
                CheckIfGameOver();
                break;
            case PENDING:
                // Do nothing - when it reached consensus, you will die
            case EMPTY:
                TheCells[cellNo].CellState = TheCellState.THEIRS;
                TheCells[cellNo].Owner = playerNo;
                ThePlayers[playerNo].squares++;
                UpdateNeighboursNeighbours(cellNo);
                SetScoreMessage();
                CheckIfTheyHaveValidMove(playerNo);
                break;
        }
    }


    // Quit game button press
    public void quitGame(View view) {
        mMessagingService.leave(null);
        super.onBackPressed();

    }

    /// make back press a controlled exit of the node
    @Override
    public void onBackPressed() {
        mMessagingService.leave(null);
        super.onBackPressed();
    }


    // Free up the babble node if thisactivity destroyed
    @Override
    protected void onDestroy() {
        mMessagingService.removeObserver(this);

        super.onDestroy();
    }


}
