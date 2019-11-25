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

public class GameActivity extends AppCompatActivity {

    int COLUMNS = 4;
    int ROWS = 8;
    int numCells = COLUMNS * ROWS;

    int PLAYERS = 4;

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
        THEIRS
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

                switch (cellsState[cellNo]) {
                    case AVAILABLE:
                        cellsState[cellNo] = CellState.PENDING;
                        ImageButton ib = (ImageButton) v;
                        ChangeCellState((ImageButton) v, cellNo, 0); // For pending neighbours is irrelevant
                        UpdateNeighbours(cellNo);

                        break;
                    case THEIRS:
                        gameState = GameState.IAMDEAD;
                        SetStatusMessage("You lose");
                        break;
                    case MINE:

                        return;
                    case PENDING:
                        // Do nothing
                        return;

                }


                ImageButton ib = (ImageButton) v;

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

              int id = getResources().getIdentifier("cell0", "drawable", this.getPackageName());

              ib.setImageResource(id);
              ib.setAdjustViewBounds(true);
              ib.setOnClickListener(listener);

        }


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
        int neighbours[] = new int[8];

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


        if (cellNo%COLUMNS < (COLUMNS - 1) ) { //MiddleRight
            neighbours[4] = cellNo  + 1;
        } else {
            neighbours[4] = -1;
        }



// Bottom 3
        if (cellNo < (numCells - COLUMNS)) {
            if (cellNo%COLUMNS > 0) { //BottomLeft
                neighbours[5] = cellNo + COLUMNS - 1;
            } else {
                neighbours[5] = -1;
            }

            neighbours[6] = cellNo + COLUMNS; //BottomCentre

            if (cellNo%COLUMNS < (COLUMNS - 1) ) { //BottomRight
                neighbours[7] = cellNo + COLUMNS + 1;
            } else {
                neighbours[7] = -1;
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
            if (neighbours[i] > 0) {
                if ((cellsState[neighbours[i]] == CellState.MINE)||
                        (cellsState[neighbours[i]] == CellState.PENDING) ) {
                    continue;
                }
                int neighbourCount = 0;
                int neighbourNeighbours[] = NeighbouringCells(neighbours[i]);
                for (int j = 0; j< neighbourNeighbours.length; j++) {
                    if (neighbourNeighbours[j] > 0) {
                        if ( (cellsState[neighbourNeighbours[j]] == CellState.PENDING) ||
                                (cellsState[neighbourNeighbours[j]] == CellState.MINE) ||
                                (cellsState[neighbourNeighbours[j]] == CellState.THEIRS)) {   //TODO change this to
                            neighbourCount++;
                        }
                    }
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
            default:
                imageName = "cell" + Integer.toString(neighbours);
        }


        int id = getResources().getIdentifier(imageName, "drawable", ib.getContext().getPackageName());
        ib.setImageResource(id);

    }


}
