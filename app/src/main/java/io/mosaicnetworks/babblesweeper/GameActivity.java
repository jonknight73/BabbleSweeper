package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    int COLUMNS = 8;
    int ROWS = 12;
    int numCells = COLUMNS * ROWS;

    int numberOfBombs = 10;

    int PLAYERS = 4;


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
    }


    Cell TheCells[];
    Player ThePlayers[];

    int MyPlayerIdx = 0;

    GameState gameState;

    public enum GameState {
        PLAYING,
        IAMDEAD,
        GAMEOVER
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Log.i(MainActivity.TAG, "GameActivity.onCreate");

        InitialiseBoard();
    }



    private void InitialiseBoard() {
        Log.i(MainActivity.TAG, "InitialiseBoard");

        int width_weight = 100/COLUMNS;
        int height_weight = 80/ROWS;


        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cellNo = (int) v.getTag();
                Log.i(MainActivity.TAG, String.format("Clicked %d", cellNo));

// If GameState is not in Playing State, you cannot do anything with the cells
                if (gameState != GameState.PLAYING) {
                    return ;
                }

                switch (TheCells[cellNo].CellState) {
                    case THEIRS:
                        gameState = GameState.IAMDEAD;
                        SetStatusMessage("Someone else is here. You lose");
                        return;
                    case BOMB:
                        gameState = GameState.IAMDEAD;
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

                for (int i = 0; i< neighbours.length; i++) {
                    if (neighbours[i] > -1 ) {
                        if ( (TheCells[neighbours[i]].CellState == TheCellState.MINE) ||
                                (TheCells[neighbours[i]].CellState == TheCellState.PENDING) ) {
                            isNeighbour = true;
                        }
                    }
                }

                if ( ! isNeighbour) { return; }

                TheCells[cellNo].CellState = TheCellState.PENDING;
                UpdateNeighbours(cellNo);

            }
        };



        TheCells = new Cell[numCells];
        ThePlayers = new Player[PLAYERS];

        for (int j = 0; j < ThePlayers.length; j++) {
            ThePlayers[j] = new Player();
        }




        TableLayout tl= (TableLayout) findViewById(R.id.tableLayout);
        TableRow tr = new TableRow(this); //TODO remove this line and suppress the resultant warning

        for (int i=0; i< numCells; i++) {

            Log.i(MainActivity.TAG, String.format("Loop Row %2d", i));

            if (i%COLUMNS == 0) {
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



        int bombCnt = 0;

        do {
           int possibleCell = new Random().nextInt(numCells);
           if ( TheCells[possibleCell].CellState == TheCellState.EMPTY ) {
               TheCells[possibleCell].CellState = TheCellState.BOMB;
               bombCnt++;
           }
        }  while (bombCnt < numberOfBombs) ;


// This will need to be deterministic for the multinode approach

        int PlayerCell = -1;
        int PlayerCnt = 0;
        do {
            int possibleCell = new Random().nextInt(numCells);
            if ( TheCells[possibleCell].CellState == TheCellState.EMPTY ) {
                if (MyPlayerIdx == PlayerCnt) {
                    TheCells[possibleCell].CellState = TheCellState.MINE;
                    PlayerCell = possibleCell;
                } else {
                    TheCells[possibleCell].CellState = TheCellState.THEIRS;
                }
                TheCells[possibleCell].Owner = PlayerCnt;
                PlayerCnt++;            }
        } while (PlayerCnt < PLAYERS );


        // Defer so all the other players are in situ
        UpdateNeighbours(PlayerCell);

        gameState = GameState.PLAYING;
        SetStatusMessage("Game On...");

    }



    private void SetStatusMessage(String msg) {
        TextView tv= (TextView) findViewById(R.id.statusText);
        tv.setText(msg);
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



    private void UpdateNeighbours(int cellNo) {
        // ROWS, COLUMNS
        int neighbours[] = NeighbouringCells(cellNo);

                int neighbourCount = 0;
                int neighbourNeighbours[] = NeighbouringCells(cellNo);
                for (int j = 0; j< neighbourNeighbours.length; j++) {
                    if (neighbourNeighbours[j] >= 0) {
                        Cell thisCell = TheCells[neighbourNeighbours[j]];

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
      //      case PENDING:
        //        imageName = "pending";
          //      break;
            case EMPTY:
                imageName = "blank";
                break;
            case BOMB:
                imageName = "blank";
            case THEIRS:
                imageName = "blank";
            default:
                int neighbours = TheCells[CellNo].Neighbours;
                imageName = "cell" + Integer.toString(neighbours);
        }


        int id = getResources().getIdentifier(imageName, "drawable", ib.getContext().getPackageName());
        ib.setImageResource(id);

    }


}
