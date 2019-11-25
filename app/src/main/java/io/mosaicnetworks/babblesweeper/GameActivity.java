package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

    boolean firstSquare = true;

    int cellsArray[];
    CellState cellsState[];
    int cellsNeighbours[];



    GameState gameState;

    public enum GameState {
        PLAYING,
        IAMDEAD,
        GAMEOVER
    }


    public enum CellState {
        AVAILABLE,
        PENDING,
        MINE,
        THEIRS,
        NEIGHBOURING,
        BOMB,
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

        firstSquare = true;
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

                switch (cellsState[cellNo]) {
                    case AVAILABLE:
                        if ( ! firstSquare) {
                            return ;
                        }
                        firstSquare = false;
                        break;
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

                cellsState[cellNo] = CellState.PENDING;
                ImageButton ib = (ImageButton) v;
                ChangeCellState((ImageButton) v, cellNo, 0); // For pending neighbours is irrelevant
                UpdateNeighbours(cellNo);

            }
        };


        cellsNeighbours = new int[numCells];
        cellsArray = new int[numCells];
        cellsState = new CellState[numCells];


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

            ImageButton ib = new ImageButton(this);


            ib.setId(View.generateViewId());


              ib.setTag(i);
              cellsArray[i] = ib.getId();
              cellsState[i] = CellState.AVAILABLE;
              cellsNeighbours[i] = 0;

              tr.addView(ib);

              ib.setLayoutParams(new TableRow.LayoutParams(0, TableLayout.LayoutParams.FILL_PARENT, width_weight));
              ib.setScaleType(ImageView.ScaleType.FIT_XY);

              int id = getResources().getIdentifier("blank", "drawable", this.getPackageName());

              ib.setImageResource(id);
              ib.setAdjustViewBounds(true);
              ib.setOnClickListener(listener);

        }



        int bombCnt = 0;

        do {
           int possibleCell = new Random().nextInt(numCells);
           if ( cellsState[possibleCell] == CellState.AVAILABLE ) {
               cellsState[possibleCell] = CellState.BOMB;
               bombCnt++;
           }
        }  while (bombCnt < numberOfBombs) ;




        gameState = GameState.PLAYING;
        SetStatusMessage("Game On...");

    }



    private void ChangeButtonState(ImageButton ib, int id, int newState) {


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

        for (int i = 0; i< neighbours.length; i++) {
            if (neighbours[i] >= 0) {
                if ((cellsState[neighbours[i]] == CellState.MINE)||
                        (cellsState[neighbours[i]] == CellState.PENDING) ) {
                    continue;
                }
                int neighbourCount = 0;
                int neighbourNeighbours[] = NeighbouringCells(neighbours[i]);
                for (int j = 0; j< neighbourNeighbours.length; j++) {
                    if (neighbourNeighbours[j] >= 0) {
                        if (
                                (cellsState[neighbourNeighbours[j]] == CellState.BOMB) ||
                                (cellsState[neighbourNeighbours[j]] == CellState.THEIRS)) {   //TODO change this to
                            neighbourCount++;
                        }
                    }
                }
                if  (cellsState[neighbours[i]] == CellState.AVAILABLE) {
                    cellsState[neighbours[i]] = CellState.NEIGHBOURING;
                }
                ChangeCellState((ImageButton)findViewById(cellsArray[neighbours[i]]), neighbours[i], neighbourCount);
            }
        }
    }




    private void ChangeCellState(ImageButton ib, int CellNo, int neighbours) {

        String imageName = "";

        switch(cellsState[CellNo]) {
            case MINE:
                imageName = "claimed";
                break;
            case PENDING:
                imageName = "pending";
                break;
            case AVAILABLE:
                imageName = "blank";
                break;
            default:
                imageName = "cell" + Integer.toString(neighbours);
        }


        int id = getResources().getIdentifier(imageName, "drawable", ib.getContext().getPackageName());
        ib.setImageResource(id);

    }


}
