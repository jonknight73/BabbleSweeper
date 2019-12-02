# BabbleSweeper

**This code is under development and you are using it at your own risk**




//TODO Here we will discuss starting a babble node. But as the babble node is not integrated in the
code yet, we will discuss that afterwards.



----

## How to Play

Open the app. There are 2 icons on the opening screen: 

//TODO image of **New**   //TODO image of **JOIN**

The **New** button creates a new Babble network. The **Join** button allows you to join an existing Babble Network.

### New Network

//TODO screenshot of new node

To start a new network, you just need to enter a moniker - a more user friendly name to identify you by. Monikers are not guaranteed to be unique -- they are only used to for display purposes.

The moniker will be prefilled with the last value entered.

Upon pressing the ``START`` button, the Game Screen loads.

### Join Network

//TODO screenshot of join node

The moniker is a nickname as described in the previous section. 

The hostname is the address of a node already in the network that you wish to join. Generally you would enter an IP address here. 

The previously entered values of moniker and hostname will be prefilled.

Upon pressing the ``JOIN`` button, the Game Screen loads.

### Game Screen

//TODO Screenshot of Initial Game Screen

Initially in the game screen you are waiting for other players to join. When you have as many players as you wish to play joined, press the start game button. 

//TODO screenshot of start game button

When you start the game, there is a X by Y grid (//TODO put the grid size in here). Your aim is to
claim as much territory as possible. Unfortunately the fog of war has descended and we do not know
what the other players are up to. The boffins have been hard at work and have come up with a gadget
to help you. It shows the number of occupied neighbouring squares. It does not differentiate between squares occupied by other players and squares booby-trapped with bombs. 

When you claim a square, you don't own it until all the other nodes in the network agree to let you have it. You may not claim another square until your pending claim is settled. 

If a square you are attempting to claim a square that already has been claimed, or contains a booby trap, then your game is over - and you get a live updating screen showing the progress of the game for the other players. 

The score is in the top right. Your score is the first one. 


## Developer Notes

The current release of Sweeper is built using version 0.2.1 of [babble-android](https://github.com/mosaicnetworks/babble-android). 

Subsequent releases have moved a fair quantity of the boilerplate code into the library. If you are looking for a model project to build a babble app, [babble-android-tutorial](https://github.com/mosaicnetworks/babble-android-tutorial) is a better choice. 

