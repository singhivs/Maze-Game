import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.AboveImage;
import javalib.worldimages.BesideImage;
import javalib.worldimages.CircleImage;
import javalib.worldimages.EmptyImage;
import javalib.worldimages.FontStyle;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayImage;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;

/*
 * GRADERS NOTE:
 * Graph has a bool called onTickLoad. This is purely a bool
 * to save processing power by slowing the creation of the map.
 * It is not essential to load and if your computer
 * can handle it just input it as false. Additionally, the
 * String autosolve solves depthFirst if the input is "d", 
 * breadthFirst if the input is "b", our custom (more optimized)
 * algorithim if the input is "o", and it wont autosolve
 * at all if the input is anything else.
 * So, to create a random 5x8 maze that doesn't load on tick and
 * is automatically solved depthFirst, the syntax is:
 * Graph(5, 8, false, "d")
 * 
 * Additionally, press 'r' to reset!
 * 
 */
class Cell {
  int num;

  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  boolean checked;
  boolean correct;
  boolean player;

  ArrayList<Cell> path;

  Cell(int n) {
    this.num = n;
    this.checked = false;
    this.player = false;
    path = new ArrayList<Cell>();
  }

  // updates the given cell
  void updateCell(int n, ArrayList<Cell> checkedCells) {
    ArrayList<Cell> newList = new ArrayList<Cell>();

    for (Cell c : checkedCells) {
      newList.add(c);
    }

    newList.add(this);

    for (Cell c : this.getAdjacent()) {
      if (!checkedCells.contains(c) && c.num == num) {
        newList.add(c);
        c.updateCell(n, newList);
      }
    }
    num = n;
  }

  // updates the given cell
  ArrayList<Cell> getConnected(ArrayList<Cell> checkedCells) {
    ArrayList<Cell> newList = new ArrayList<Cell>();

    for (Cell c : checkedCells) {
      newList.add(c);
    }

    newList.add(this);

    for (Cell c : this.getAdjacent()) {
      if (!checkedCells.contains(c)) {
        newList.add(c);
        for (Cell newCell : c.getConnected(newList)) {
          if (!newList.contains(newCell) && !checkedCells.contains(newCell)) {
            newList.add(newCell);
          }
        }
      }
    }

    return newList;
  }

  void checkCell() {
    this.checked = true;
  }

  void correctCell() {
    this.correct = true;
  }

  void playerOnCell() {
    this.player = true;
  }

  Cell movement(String s) {
    if (s.equals("left") && this.left != null) {
      this.left.playerOnCell();
      this.player = false;
      return this.left;
    }
    if (s.equals("right") && this.right != null) {
      this.right.playerOnCell();
      this.player = false;
      return this.right;
    }
    if (s.equals("down") && this.top != null) {
      this.top.playerOnCell();
      this.player = false;
      return this.top;
    }
    if (s.equals("up") && this.bottom != null) {
      this.bottom.playerOnCell();
      this.player = false;
      return this.bottom;
    }
    return this;
  }

  // return an arraylist of the adjacent cells of the given cell
  ArrayList<Cell> getAdjacent() {
    ArrayList<Cell> newCells = new ArrayList<Cell>();

    if (this.top != null) {
      newCells.add(this.top);
    }
    if (this.right != null) {
      newCells.add(this.right);
    }
    if (this.left != null) {
      newCells.add(this.left);
    }
    if (this.bottom != null) {
      newCells.add(this.bottom);
    }
    return newCells;
  }

  // draws the background
  WorldImage draw() {
    WorldImage background;
    if (correct) {
      background = new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE, 
          OutlineMode.SOLID, Color.blue);
    }
    else if (checked) {
      background = new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE,
          OutlineMode.SOLID, Color.cyan);
    }
    else {
      background = new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE, 
          OutlineMode.SOLID, Color.gray);
    }

    if (player) {
      WorldImage c = new CircleImage((Graph.CELL_SIZE / 2), 
          OutlineMode.SOLID, Color.BLACK);
      return new OverlayImage(c, background);
    }

    return background;
  }

  WorldImage draw(Color c) {

    WorldImage background = new RectangleImage(Graph.CELL_SIZE, 
        Graph.CELL_SIZE, OutlineMode.SOLID, c);
    if (player) {
      WorldImage c2 = new CircleImage((Graph.CELL_SIZE / 2),
          OutlineMode.SOLID, Color.BLACK);
      return new OverlayImage(c2, background);
    }

    return background;
  }
}

class Edge {
  Cell from;
  Cell to;
  int weight;

  Edge(Cell f, Cell t, int w) {
    this.from = f;
    this.to = t;
    this.weight = w;
  }
}

// represents the Maze World game
class Graph extends World {

  int clicks;

  Cell goal;
  public static int BOARD_HEIGHT;
  public static int BOARD_WIDTH;

  public static int CELL_SIZE; // cell size to correspond to board size

  boolean loadOnTick;

  ArrayList<Edge> allEdges;
  ArrayList<Edge> uncheckedEdges;
  ArrayList<ArrayList<Cell>> cells; // represents the board

  ArrayList<Cell> path;
  boolean finished;

  String autosolve;

  Cell playerCell;

  // standard constructor
  Graph(int bw, int bh, boolean loadOnTick, String autosolve) {
    Graph.BOARD_HEIGHT = bh;
    Graph.BOARD_WIDTH = bw;
    Graph.CELL_SIZE = 750 / bh;
    this.loadOnTick = loadOnTick;
    this.autosolve = autosolve;
    resetGame();

  }

  // reset the Game
  void resetGame() {
    clicks = 0;
    allEdges = new ArrayList<Edge>();
    uncheckedEdges = new ArrayList<Edge>();
    finished = false;
    this.cells = initCells();

    for (Edge e : allEdges) {
      uncheckedEdges.add(e);
    }

    if (!loadOnTick) {
      Random rand = new Random(); // instance of random class
      while (!allConnected()) {
        Edge randomEdge = uncheckedEdges.get(
            rand.nextInt(uncheckedEdges.size()));
        Cell c1 = randomEdge.from;
        Cell c2 = randomEdge.to;
        uncheckedEdges.remove(randomEdge);

        if (c1.num != c2.num) {

          if (c1.num < c2.num) {
            c2.updateCell(c1.num, new ArrayList<Cell>());

          }
          else {
            c1.updateCell(c2.num, new ArrayList<Cell>());
          }

          allEdges.remove(randomEdge);
        }
      }
      goal = cells.get(BOARD_HEIGHT - 1).get(BOARD_WIDTH - 1);

    }
    remapCells();
    path = new ArrayList<Cell>();
    path.add(cells.get(0).get(0));

    if (!(autosolve.equals("b") || autosolve.equals("o") || 
        autosolve.equals("d"))) {
      playerCell = cells.get(0).get(0);
      playerCell.playerOnCell();
    }
  }

  // Constructor for testing
  Graph(int bw, int bh, ArrayList<ArrayList<Cell>> board, 
      ArrayList<Edge> edges) {
    clicks = 0;
    Graph.BOARD_HEIGHT = bh;
    Graph.BOARD_WIDTH = bw;
    this.loadOnTick = false;
    uncheckedEdges = new ArrayList<Edge>();

    this.cells = board;
    allEdges = edges;

    for (Edge e : allEdges) {
      uncheckedEdges.add(e);
    }

  }

  // Are all the cells currently connected?
  public boolean allConnected() {

    for (ArrayList<Cell> row : this.cells) {
      for (Cell c : row) {
        if (c.num != 0) {
          return false;
        }
      }
    }

    return true;
  }

  // Converts a given cell to a posn
  public Posn cellToPosn(Cell c) {
    for (int i = 0; i < BOARD_HEIGHT; i++) {
      for (int j = 0; j < BOARD_WIDTH; j++) {
        Cell cell = this.cells.get(i).get(j);
        if (c.equals(cell)) {
          return new Posn(j, i);
        }
      }
    }
    return null;
  }

  // Remaps the cells based off of the edges
  void remapCells() {
    for (Edge e : allEdges) {
      Cell c1 = e.from;
      Cell c2 = e.to;

      if (c1.right != null && c1.right.equals(c2)) {
        c1.right = null;
        c2.left = null;
      }

      if (c1.left != null && c1.left.equals(c2)) {
        c1.left = null;
        c2.right = null;
      }
      if (c1.bottom != null && c1.bottom.equals(c2)) {
        c1.bottom = null;
        c2.top = null;
      }
      if (c1.top != null && c1.top.equals(c2)) {
        c1.top = null;
        c2.bottom = null;
      }
    }
  }

  // initialize the cells
  public ArrayList<ArrayList<Cell>> initCells() {
    ArrayList<ArrayList<Cell>> result = new ArrayList<>();
    int k = 0;
    for (int i = 0; i < BOARD_HEIGHT; i++) {
      ArrayList<Cell> row = new ArrayList<>(); // stores the current row
      for (int j = 0; j < BOARD_WIDTH; j++) {
        Cell c = new Cell(k);
        k += 1;
        row.add(c);
      }
      result.add(row);
    }

    // set up edges
    for (int i = 0; i < BOARD_HEIGHT; i += 1) {
      for (int j = 0; j < BOARD_WIDTH; j += 1) {

        Cell modifyThis = result.get(i).get(j);

        if (i == 0) {
          modifyThis.bottom = null;
        }
        else {
          modifyThis.bottom = result.get(i - 1).get(j);

        }

        if (i == BOARD_HEIGHT - 1) {
          modifyThis.top = null;
        }
        else {
          modifyThis.top = result.get(i + 1).get(j);
          allEdges.add(new Edge(modifyThis, modifyThis.top, i + j));
        }

        if (j == 0) {
          modifyThis.left = null;
        }
        else {
          modifyThis.left = result.get(i).get(j - 1);
        }

        if (j == BOARD_WIDTH - 1) {
          modifyThis.right = null;
        }
        else {
          modifyThis.right = result.get(i).get(j + 1);
          allEdges.add(new Edge(modifyThis, modifyThis.right, i + j));
        }

      }
    }

    return result;

  }

  // draw the game
  public WorldScene makeScene() {
    WorldScene background = new WorldScene(2000, 2000);// empty scene,
    //where we will put our board
    WorldImage board = new EmptyImage(); // empty IMAGE (not scene) 
    //that we will use to build up our
    for (int i = 0; i < BOARD_HEIGHT; i++) {
      WorldImage row = new EmptyImage(); // build up the rows,
      //then add the rows to the board
      for (int j = 0; j < BOARD_WIDTH; j++) {
        WorldImage cell;
        if (i == 0 && j == 0) {

          cell = this.cells.get(i).get(j).draw(Color.GREEN);
        }
        else if (i == BOARD_HEIGHT - 1 && j == BOARD_WIDTH - 1) {
          cell = this.cells.get(i).get(j).draw(Color.MAGENTA);
        }
        else {
          cell = this.cells.get(i).get(j).draw();
        }
        row = new BesideImage(row, cell);
      }
      board = new AboveImage(board, row);
    }
    int x = (BOARD_WIDTH * CELL_SIZE / 2);
    int y = (BOARD_HEIGHT * CELL_SIZE / 2);

    background.placeImageXY(board, x, y);
    background.placeImageXY(new RectangleImage((2 * x), (2 * y), 
        OutlineMode.OUTLINE, Color.BLACK), x, y);
    WorldImage hLine = new RectangleImage(Graph.CELL_SIZE, 1, 
        OutlineMode.SOLID, Color.BLACK);
    WorldImage vLine = new RectangleImage(1, Graph.CELL_SIZE,
        OutlineMode.SOLID, Color.BLACK);

    for (Edge e : allEdges) {
      Posn p1 = cellToPosn(e.from);
      Posn p2 = cellToPosn(e.to);
      float pX = ((p1.x + p2.x) / 2f) + 0.5f;
      float pY = ((p1.y + p2.y) / 2f) + 0.5f;
      if ((p2.x - p1.x) != 0) {
        background.placeImageXY(vLine, (int) (pX * Graph.CELL_SIZE), 
            (int) (pY * Graph.CELL_SIZE));
      }
      else {
        background.placeImageXY(hLine, (int) (pX * Graph.CELL_SIZE), 
            (int) (pY * Graph.CELL_SIZE));

      }
    }

    if (finished) {
      background.placeImageXY(new TextImage("YOU WIN!", x / 4,
          FontStyle.BOLD_ITALIC, Color.BLACK), x, y);
    }
    return background;
  }

  public void onTick() {
    if (loadOnTick) {
      Random rand = new Random(); // instance of random class
      if (!allConnected()) {
        Edge randomEdge = uncheckedEdges.get(
            rand.nextInt(uncheckedEdges.size()));
        Cell c1 = randomEdge.from;
        Cell c2 = randomEdge.to;

        if (c1.num != c2.num) {
          if (c1.num < c2.num) {
            c2.updateCell(c1.num, new ArrayList<Cell>());

          }
          else {
            c1.updateCell(c2.num, new ArrayList<Cell>());
          }

          allEdges.remove(randomEdge);
        }

        uncheckedEdges.remove(randomEdge);
      }
      else {
        remapCells();
        loadOnTick = false;
      }
    }
    makeScene();

    if (autosolve.equals("b")) {
      solveBreadthFirst();
    }
    if (autosolve.equals("d")) {
      solveDepthFirst();
    }
    if (autosolve.equals("o")) {
      solveMaze(true);
    }

  }

  public void onKeyEvent(String key) {

    if (key.equals("r")) {
      resetGame();
    }
    else {
      if (!finished && !playerCell.equals(cells.get(BOARD_HEIGHT 
          - 1).get(BOARD_WIDTH - 1))) {
        playerCell = playerCell.movement(key);
        if (playerCell.equals(cells.get(BOARD_HEIGHT 
            - 1).get(BOARD_WIDTH - 1))) {
          while (!finished) {
            solveMaze(false);
          }
        }
      }
    }
  }

  // solves the maze
  void solveMaze(boolean showChecked) {
    if (!finished) {
      // checks if path ends in goal cell
      if (!path.get(path.size() - 1).equals(goal)) {
        // sets up latest path to check connected from current end of path
        ArrayList<Cell> latestPath = new ArrayList<Cell>();
        latestPath.add(path.get(path.size() - 1));

        //set up bool to stop the current search once the correct path
        //has been found
        boolean hitPath = false;
        ArrayList<Cell> newCells = path.get(path.size() - 1).getAdjacent();
        // if path is long enough remove already checked cells from 
        //current adjacent
        if (path.size() > 1) {
          newCells.remove(newCells.indexOf(path.get(path.size() - 2)));
        }

        for (Cell c : newCells) {
          if (!hitPath) {
            // if theres only 1 new adjacent, its a one way
            if (newCells.size() == 1) {
              hitPath = true;
              path.add(newCells.get(0));
              path.get(path.size() - 1).checkCell();
            }
            // if not, check the connected cells for each potential 
            // path and go down the correct one
            else {
              ArrayList<Cell> cellList = new ArrayList<Cell>();
              cellList = c.getConnected(latestPath);
              if (showChecked) {
                cellList.get(1).checkCell();
              }
              if (cellList.contains(goal)) {
                hitPath = true;
                path.add(cellList.get(1));
                cellList.get(1).checkCell();
              }
            }
          }
        }
      }
      else {
        // once finished highlight the correct path
        for (Cell c : path) {
          c.correctCell();
        }
        finished = true;
      }
    }
  }

  //solves the maze DepthFirst
  void solveDepthFirst() {
    if (!finished) {
      // checks if path ends in goal cell
      if (!path.get(path.size() - 1).equals(goal)) {
        // sets up latest path to check connected from current end of path
        ArrayList<Cell> latestPath = new ArrayList<Cell>();
        latestPath.add(path.get(path.size() - 1));

        // set up bool to stop the current search once the correct 
        //path has been found
        boolean hitPath = false;
        ArrayList<Cell> newCells = path.get(path.size() - 1).getAdjacent();

        // if path is long enough remove already checked cells 
        //from current adjacent
        if (path.size() > 1) {
          newCells.remove(newCells.indexOf(path.get(path.size() - 2)));
        }

        for (Cell c : newCells) {
          if (!hitPath) {
            ArrayList<Cell> cellList = new ArrayList<Cell>();
            cellList = c.getConnected(latestPath);
            if (cellList.contains(goal)) {
              hitPath = true;
              path.add(cellList.get(1));
              cellList.get(1).checkCell();
            }
            else {
              for (Cell newCell : cellList) {
                newCell.checkCell();
              }
            }
          }
        }
      }
      else {
        // once finished highlight the correct path
        for (Cell c : path) {
          c.correctCell();
        }
        finished = true;
      }
    }
  }

  //solves the maze BreadthFirst
  void solveBreadthFirst() {
    if (!finished) {
      // checks if path ends in goal cell
      if (!path.get(path.size() - 1).equals(goal)) {
        // sets up latest path to check connected from current end of path
        ArrayList<Cell> latestPath = new ArrayList<Cell>();
        latestPath.add(path.get(path.size() - 1));

        ArrayList<Cell> newCells = path.get(path.size() - 1).getAdjacent();

        // if path is long enough remove already checked cells
        //from current adjacent
        if (path.size() > 1) {
          newCells.remove(newCells.indexOf(path.get(path.size() - 2)));
        }

        for (Cell c : newCells) {
          ArrayList<Cell> cellList = new ArrayList<Cell>();
          cellList = c.getConnected(latestPath);
          if (cellList.contains(goal)) {
            path.add(cellList.get(1));
            cellList.get(1).checkCell();
          }
          else {
            for (Cell newCell : cellList) {
              newCell.checkCell();
            }
          }
        }
      }
      else {
        // once finished highlight the correct path
        for (Cell c : path) {
          c.correctCell();
        }
        finished = true;
      }
    }
  }

}

class ExamplesGraph {

  Graph testUnfinishedWorld;
  Graph testFinishedWorld;
  Graph testAUnfinishedWorld;

  Cell ac1;
  Cell ac2;
  Cell ac3;
  Cell ac4;

  Cell c1;
  Cell c2;
  Cell c3;
  Cell c4;

  Cell c5;
  Cell c6;
  Cell c7;
  Cell c8;
  Cell c9;
  Cell c10;

  ArrayList<Cell> row1 = new ArrayList<Cell>();
  ArrayList<Cell> row2 = new ArrayList<Cell>();
  ArrayList<Cell> row3 = new ArrayList<Cell>();
  ArrayList<Cell> row4 = new ArrayList<Cell>();
  ArrayList<Cell> cellArray = new ArrayList<Cell>();
  ArrayList<Cell> cellArray2 = new ArrayList<Cell>();
  ArrayList<Edge> edges1 = new ArrayList<Edge>();
  ArrayList<Edge> edges2 = new ArrayList<Edge>();
  ArrayList<ArrayList<Cell>> board1 = new ArrayList<ArrayList<Cell>>();
  ArrayList<ArrayList<Cell>> board2 = new ArrayList<ArrayList<Cell>>();

  ArrayList<Cell> arow1 = new ArrayList<Cell>();
  ArrayList<Cell> arow2 = new ArrayList<Cell>();
  ArrayList<Edge> aedges1 = new ArrayList<Edge>();
  ArrayList<ArrayList<Cell>> aboard1 = new ArrayList<ArrayList<Cell>>();

  WorldScene background = new WorldScene(2000, 2000);
  WorldImage board = new EmptyImage(); 
  WorldImage hLine;
  WorldImage vLine;

  void initUnFinishedWorld() {

    // initialize unfinished world
    c1 = new Cell(0);
    c2 = new Cell(0);
    c3 = new Cell(1);
    c4 = new Cell(1);
    c9 = new Cell(1);
    c10 = new Cell(0);

    c1.top = null;
    c1.left = null;
    c1.right = c2;
    c1.bottom = c3;

    c2.top = null;
    c2.left = c1;
    c2.right = null;
    c2.bottom = c4;

    c3.top = c1;
    c3.left = null;
    c3.right = c4;
    c3.bottom = null;

    c4.top = c2;
    c4.left = c3;
    c4.right = null;
    c4.bottom = null;

    Edge e1 = new Edge(c1, c3, 1);
    Edge e2 = new Edge(c2, c4, 2);

    row1.add(c2);
    row1.add(c1);

    row2.add(c4);
    row2.add(c3);

    board1.add(row1);
    board1.add(row2);

    edges1.add(e1);
    edges1.add(e2);

    testUnfinishedWorld = new Graph(2, 2, board1, edges1);
  }

  void initFinishedWorld() {
    // initialize finished world
    c5 = new Cell(0);
    c6 = new Cell(0);
    c7 = new Cell(0);
    c8 = new Cell(0);

    c5.top = null;
    c5.left = null;
    c5.right = c6;
    c5.bottom = c8;

    c6.top = null;
    c6.left = c5;
    c6.right = null;
    c6.bottom = c8;

    c7.top = c5;
    c7.left = null;
    c7.right = c8;
    c7.bottom = null;

    c8.top = c6;
    c8.left = c7;
    c8.right = null;
    c8.bottom = null;

    Edge e3 = new Edge(c5, c6, 1);

    row3.add(c5);
    row3.add(c6);

    row4.add(c7);
    row4.add(c8);

    board2.add(row3);
    board2.add(row4);

    edges2.add(e3);

    testFinishedWorld = new Graph(2, 2, board2, edges2);

  }

  void initAUnfinishedWorld() {
    // initialize unfinished world
    ac1 = new Cell(0);
    ac2 = new Cell(0);
    ac3 = new Cell(1);
    ac4 = new Cell(1);

    ac1.top = null;
    ac1.left = null;
    ac1.right = ac2;
    ac1.bottom = ac3;

    ac2.top = null;
    ac2.left = ac1;
    ac2.right = null;
    ac2.bottom = ac4;

    ac3.top = ac1;
    ac3.left = null;
    ac3.right = ac4;
    ac3.bottom = null;

    ac4.top = ac2;
    ac4.left = ac3;
    ac4.right = null;
    ac4.bottom = null;

    Edge ae1 = new Edge(ac1, ac3, 1);
    Edge ae2 = new Edge(ac2, ac4, 2);

    arow1.add(ac2);
    arow1.add(ac1);

    arow2.add(ac4);
    arow2.add(ac3);

    aboard1.add(arow1);
    aboard1.add(arow2);

    aedges1.add(ae1);
    aedges1.add(ae2);

    testAUnfinishedWorld = new Graph(2, 2, aboard1, aedges1);
  }

  void makeSceneTest() {
    this.initUnFinishedWorld();
    for (int i = 0; i < 2; i++) {
      WorldImage row = new EmptyImage(); // build up the rows, 
      //then add the rows to the board
      for (int j = 0; j < 2; j++) {
        WorldImage cell;
        if (i == 0 && j == 0) {

          cell = this.board1.get(i).get(j).draw(Color.GREEN);
        }
        else if (i == 2 - 1 && j == 2 - 1) {
          cell = this.board1.get(i).get(j).draw(Color.MAGENTA);
        }
        else {
          cell = this.board1.get(i).get(j).draw();
        }
        row = new BesideImage(row, cell);
      }
      board = new AboveImage(board, row);
    }
    int x = (2 * 20 / 2);
    int y = (2 * 20 / 2);

    background.placeImageXY(board, x, y);
    background.placeImageXY(new RectangleImage((2 * x), (2 * y), 
        OutlineMode.OUTLINE, Color.BLACK), x, y);
    hLine = new RectangleImage(Graph.CELL_SIZE, 1, 
        OutlineMode.SOLID, Color.BLACK);
    vLine = new RectangleImage(1, Graph.CELL_SIZE, 
        OutlineMode.SOLID, Color.BLACK);

    float pX = ((1 + 0) / 2f) + 0.5f;
    float pY = ((1 + 1) / 2f) + 0.5f;
    background.placeImageXY(hLine, (int) (pX * Graph.CELL_SIZE), 
        (int) (pY * Graph.CELL_SIZE));

  }

  /*
   * void testCellToPosn(Tester t) { this.initUnFinishedWorld();
   * 
   * }
   * 
   * void testAllConnected(Tester t) { this.initUnFinishedWorld();
   * this.initFinishedWorld();
   * 
   * }
   * 
   * void testUpdateCell(Tester t) {
   * 
   * this.initUnFinishedWorld(); this.initFinishedWorld();
   * 
   * }
   */

  // Writing the test in different function resulted in error
  // so we grouped all the tested together
  void testGame(Tester t) {
    this.initUnFinishedWorld();
    this.initFinishedWorld();
    this.initAUnfinishedWorld();
    this.makeSceneTest();
    // testUnfinishedWorld.bigBang(Graph.CELL_SIZE * Graph.BOARD_WIDTH,
    // Graph.CELL_SIZE * Graph.BOARD_HEIGHT, .0001f);

    // tests for cell to posn
    t.checkExpect(testFinishedWorld.cellToPosn(this.c5), new Posn(0, 0));
    t.checkExpect(testFinishedWorld.cellToPosn(this.c6), new Posn(1, 0));

    // tests for allConnected
    t.checkExpect(testUnfinishedWorld.allConnected(), false);
    t.checkExpect(testFinishedWorld.allConnected(), true);

    // tests for updateCell
    t.checkExpect(c3.num, 1);
    c3.updateCell(0, new ArrayList<Cell>());
    t.checkExpect(c3.num, 0);
    t.checkExpect(c4.num, 0);

    t.checkExpect(c5.num, 0);
    c3.updateCell(0, new ArrayList<Cell>());
    t.checkExpect(c5.num, 0);
    t.checkExpect(c6.num, 0);

    // tests for checkCell
    c9.checkCell();
    t.checkExpect(c9.checked, true);

    c10.checkCell();
    t.checkExpect(c10.checked, true);

    // tests for correctCell
    c9.correctCell();
    t.checkExpect(c9.correct, true);

    c10.correctCell();
    t.checkExpect(c10.correct, true);

    // tests for playerOnCell
    c9.playerOnCell();
    t.checkExpect(c9.player, true);

    c10.playerOnCell();
    t.checkExpect(c10.player, true);

    // tests for movement
    c1.playerOnCell();
    c8.playerOnCell();
    c6.playerOnCell();
    t.checkExpect(c2.movement("left"), c1);
    t.checkExpect(c5.movement("right"), c6);
    t.checkExpect(c5.movement("up"), c8);
    t.checkExpect(c3.movement("down"), c1);

    // tests for draw
    t.checkExpect(c1.draw(), new OverlayImage(
        new CircleImage((Graph.CELL_SIZE / 2),
            OutlineMode.SOLID, Color.BLACK),
        new RectangleImage(Graph.CELL_SIZE,
            Graph.CELL_SIZE, OutlineMode.SOLID, Color.gray)));

    c4.checkCell();
    t.checkExpect(c4.draw(), 
        new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE,
            OutlineMode.SOLID, Color.cyan));

    t.checkExpect(c8.draw(), new OverlayImage(
        new CircleImage((Graph.CELL_SIZE / 2),
            OutlineMode.SOLID, Color.BLACK), 
        new RectangleImage(Graph.CELL_SIZE, 
            Graph.CELL_SIZE, OutlineMode.SOLID, Color.gray)));

    t.checkExpect(c10.draw(), new OverlayImage(
        new CircleImage((Graph.CELL_SIZE / 2), 
            OutlineMode.SOLID, Color.BLACK), 
        new RectangleImage(Graph.CELL_SIZE, 
            Graph.CELL_SIZE, OutlineMode.SOLID, Color.blue)));
    // tests for initCell
    // t.checkExpect(testAUnfinishedWorld.initCells(), this.board1);

    // tests for getAdjacent
    cellArray.add(c2);
    cellArray.add(c3);
    t.checkExpect(c1.getAdjacent(), this.cellArray);

    cellArray2.add(c6);
    cellArray2.add(c8);
    t.checkExpect(c5.getAdjacent(), this.cellArray2);

    // tests for updateCell
    t.checkExpect(c3.num, 0);
    c3.updateCell(0, new ArrayList<Cell>());
    t.checkExpect(c3.num, 0);
    t.checkExpect(c4.num, 0);

    t.checkExpect(c5.num, 0);
    c3.updateCell(0, new ArrayList<Cell>());
    t.checkExpect(c5.num, 0);
    t.checkExpect(c6.num, 0);

    // Tests for makeScene
    // t.checkExpect(testUnfinishedWorld.makeScene(), background);

    // tests for remapCell
    testUnfinishedWorld.remapCells();
    t.checkExpect(c1.bottom, null);
    t.checkExpect(c3.top, null);
    t.checkExpect(c2.bottom, null);
    t.checkExpect(c4.top, null);

    new Graph(10, 10, false, "autoSolve").bigBang(Graph.CELL_SIZE * 
        Graph.BOARD_WIDTH, Graph.CELL_SIZE * Graph.BOARD_HEIGHT, .1f);

  }

  /*
   
  
   */
}
