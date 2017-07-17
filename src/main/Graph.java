package main; /**
 * Created by atuladhar on 6/27/17.
 */
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Graph {
  private PApplet parent;

  private int GENERAL_GATES;
  private int ENTRANCE;
  private int RANGER_STOPS;
  private int CAMPING;
  private int GATES;
  private int RANGER_BASE;

  private int WHITE;
  private int BLACK;

  private final Map<String, String> rename = new HashMap<String, String>() {
    {
      put("camping6", "camping5");
      put("camping3", "camping2");
      put("camping4", "camping3");
      put("camping5", "camping4");
      put("camping2", "camping1");
      put("camping8", "camping6");
      put("camping1", "camping8");
    }
  };
  private int width;
  private Map<String, Integer> sensorCounts;
  Map<String, Node> namedNodes;
  private PImage mapImage;

  private Set<Node> gatesToLabel;

  Graph(PApplet p, PImage img) {
    this.parent = p;
    System.out.println("here: " + p.getClass());
    this.mapImage = img;
    this.width = img.width;
    this.namedNodes = new LinkedHashMap<String, Node>();
    this.sensorCounts = new LinkedHashMap<String, Integer>();
    this.gatesToLabel = new HashSet<Node>();
    setColors();
  }

  private void setColors(){
    GENERAL_GATES = parent.color(0.0f, 255.0f, 255.0f);  // blue
    ENTRANCE = parent.color(76.0f, 255.0f, 0.0f);        // green
    RANGER_STOPS = parent.color(255.0f, 216.0f, 0.0f);   // yellow
    CAMPING = parent.color(255.0f, 106.0f, 0.0f);        // orange
    GATES = parent.color(255.0f, 0.0f, 0.0f);            // red
    RANGER_BASE = parent.color(255.0f, 0.0f, 220.0f);    // pink

    WHITE = parent.color(255.0f, 255.0f, 255.0f);        // white to check non road pixels.
    BLACK = parent.color(0.0f, 0.0f, 0.0f);              // to check road pixels.
  }

  int getWidth(){
    return this.width;
  }

  private int addSensor(String s){
    int count = sensorCounts.containsKey(s) ? sensorCounts.get(s): 0;
    sensorCounts.put(s, count+1);
    return count;
  }

  /** Return list of neighbours.
   tl | t | tr
   l  | p | r
   bl | b | br
   */
  public List<Integer> findNeighbours(int p) {
    int x = p % this.width;
    int y = p / this.width;

    List neighbours = new LinkedList<Integer>();

    //addNeighbour(x-1, y-1, neighbours);    // tl
    addNeighbour(x, y-1, neighbours);      // t
    //addNeighbour(x+1, y-1, neighbours);    // tr

    addNeighbour(x-1, y, neighbours);      // l
    addNeighbour(x+1, y, neighbours);      // r

    //addNeighbour(x-1, y+1, neighbours);    // bl
    addNeighbour(x, y+1, neighbours);      // b
    //addNeighbour(x+1, y+1, neighbours);    // br

    return neighbours;
  }

  private void addNeighbour(Integer x, Integer y, List neighbours) {
    if (x < 0 || y < 0 ) { return; }

    Integer p = x + y*this.width;
    if(mapImage.pixels[p] != WHITE) {
      neighbours.add(p);
    }
  }

  public void addNamedNode(Node n) {
    //this.namedNodes.put(n.getLabel() != null ? n.getLabel() : Integer.toString(n.getPixel()), n);
    this.namedNodes.put(n.getName(), n);
  }

  public Map<String, Node> getNamedNodes() {
    return this.namedNodes;
  }

  //todo : Name nodes.
  public void findLandMarks(){
    for (Map.Entry<String, Node> n : this.getNamedNodes().entrySet()){
      Node node = n.getValue();
      if(node.getNodeColor() == GENERAL_GATES) {
        node.setLabel("general-gate"+Integer.toString(addSensor("general-gate")));
      } else if(node.getNodeColor() == ENTRANCE) {
        node.setLabel("entrance"+Integer.toString(addSensor("entrance")));
      } else if(node.getNodeColor() == RANGER_STOPS) {
        node.setLabel("ranger-stop"+Integer.toString(addSensor("ranger-stop")));
      } else if(node.getNodeColor() == CAMPING) {
        String l = "camping"+Integer.toString(addSensor("camping"));
        if (rename.containsKey(l)) {
          node.setLabel(rename.get(l));
        } else {
          node.setLabel(l);
        }
      } else if(node.getNodeColor() == GATES) {
        node.setLabel("gate"+Integer.toString(addSensor("gate")));
      } else if (node.getNodeColor() == RANGER_BASE){
        node.setLabel("ranger-base");
      }
    }
  }


  void draw(int scale) {
    for (Map.Entry<String, Node> n : this.getNamedNodes().entrySet()){
      Node node = n.getValue();
      parent.fill(node.getNodeColor());
      parent.ellipse(node.x * scale, node.y * scale, 5, 5);
      if(node.getLabel() != null){
        parent.text(node.getLabel(), node.x * scale + 6, node.y * scale + 6);
      }

      // Draw Edges.
      for(Edge e: node.getNeighbours()){
        parent.fill(parent.color(0,0,0));
        if(e.path.isEmpty()){
          parent.line(e.source.x * scale, e.source.y * scale, e.target.x * scale, e.target.y * scale);
        } else {
          for(Integer i : e.path) {
            int x = i % this.width;
            int y = i / this.width;
            parent.ellipse(x * scale, y * scale, 2, 2);
          }
        }
      }
    }
  }

  /*void drawPathFromNewJson(JSONArray pathNodes, int scale) {
    int multipleEntryCount = 1;

    JSONObject currentNodeObj = pathNodes.getJSONObject(pathNodes.size() - 1);  // data is in reverse order. Therefore, looping backwards
    for(int i = pathNodes.size() - 2; i >= 0; i--) {
      String currentNodeName = currentNodeObj.getString("gate");

      JSONObject nextNodeObj = pathNodes.getJSONObject(i);
      String nextNodeName = nextNodeObj.getString("gate");

      if (this.getNamedNodes().containsKey(currentNodeName)) {
        Node currentNode = this.getNamedNodes().get(currentNodeName);
        drawNode(currentNode, scale);
        if (this.getNamedNodes().containsKey(nextNodeName)) {
          List<Edge> edgeList = currentNode.getAllEdges(nextNodeName);
          int edgeCount = 0;
          for (Edge e : edgeList) {
            edgeCount++;
            if (e != null){
              multipleEntryCount = 1;
              if (edgeCount == 1) drawEdge(e, scale);
              else drawEdgeDup(e, scale, parent.color(150*edgeCount % 255, 0,  0));
//              float speed = ((float)12/200 * e.pixelDistance * 60 * 60) / nextNodeObj.getInt("_3");
              float speed = nextNodeObj.getInt("speed");
              int x = (currentNode.x + 2) * scale;
              int y = (currentNode.y - 2 * edgeCount) * scale;
              parent.text("Speed: " + speed + " mph", x, y);
            }
            drawNode(this.getNamedNodes().get(nextNodeName), scale);
          }
          if (currentNodeName.equals(nextNodeName)){
            multipleEntryCount++;

            int x = (currentNode.x + 2) * scale;
            int y = (currentNode.y - 2) * scale;

            parent.text("X" + multipleEntryCount, x, y);
          }
        } else {
          parent.fill(parent.color(255, 0, 0));
          int x = (currentNode.x + 12) * scale;
          int y = (currentNode.y - 2) * scale;
          parent.line(x , y, (x + 4), (y + 4));
          parent.line(x , (y + 4), (x + 4), y);
          parent.text(nextNodeName, x + 12, y + 12);
          if (i > 0) currentNodeObj = (JSONObject) pathNodes.getJSONObject(i-1);
          i--;
          continue;
        }
      } else {
        System.out.println("Did not find main.Node: " + currentNodeName);
      }
      currentNodeObj = nextNodeObj;
    }
  }*/

  /*void drawPathFromNewJson(String c, JSONArray pathNodes, int scale) {
    SUBWAY_COLOR = c;
    int multipleEntryCount = 1;

    JSONObject currentNodeObj = pathNodes.getJSONObject(0);

    for(int i = 1; i < pathNodes.size(); i++) {
      String currentNodeName = currentNodeObj.getString("gate");

      JSONObject nextNodeObj = pathNodes.getJSONObject(i);
      String nextNodeName = nextNodeObj.getString("gate");

      if (this.getNamedNodes().containsKey(currentNodeName)) {
        Node currentNode = this.getNamedNodes().get(currentNodeName);
        drawNode(currentNode, scale);
        if (this.getNamedNodes().containsKey(nextNodeName)) {
          List<Edge> edgeList = currentNode.getAllEdges(nextNodeName);
          int edgeCount = 0;
          *//*for (Edge e : edgeList) {
            edgeCount++;
            if (e != null){
              multipleEntryCount = 1;
              if (edgeCount == 1) drawEdge(e, scale);
              else drawEdgeDup(e, scale, parent.color(150*edgeCount % 255, 0,  0));
              float speed = nextNodeObj.getInt("speed");
              int x = (currentNode.x + 2) * scale;
              int y = (currentNode.y - 2 * edgeCount) * scale;
              parent.text("Speed: " + speed + " mph", x, y);
            }
            drawNode(this.getNamedNodes().get(nextNodeName), scale);
          }*//*

          for (Edge e : edgeList) {
            edgeCount++;
            if (e != null){
              multipleEntryCount = 1;
              if (edgeCount == 1) drawEdge(e, scale);
              else drawEdgeDup(e, scale, parent.color(150*edgeCount % 255, 0,  0));
            }
            drawNode(this.getNamedNodes().get(nextNodeName), scale);
          }
          if (currentNodeName.equals(nextNodeName)){
            multipleEntryCount++;

            int x = (currentNode.x + 2) * scale;
            int y = (currentNode.y - 2) * scale;

            parent.text("X" + multipleEntryCount, x, y);
          }
        } else {
          parent.fill(parent.color(255, 0, 0));
          int x = (currentNode.x + 12) * scale;
          int y = (currentNode.y - 2) * scale;
          parent.line(x , y, (x + 4), (y + 4));
          parent.line(x , (y + 4), (x + 4), y);
          parent.text(nextNodeName, x + 12, y + 12);
          if (i < pathNodes.size() - 1) currentNodeObj = pathNodes.getJSONObject(i+1);
          i++;
          continue;
        }
      } else {
        System.out.println("Did not find main.Node: " + currentNodeName);
      }
      currentNodeObj = nextNodeObj;
    }
  }*/

  private void setColors(String c) {
    String[] split = c.split(",");
    int r = Integer.parseInt(split[0]);
    int g = Integer.parseInt(split[1]);
    int b = Integer.parseInt(split[2]);

    parent.stroke(r, g, b);
    parent.fill(r, g, b);
  }

  boolean flag = false;
  void drawActualPath(String[] c, String [] multipleDayPath, int scale) {

    String mainColor;
    if (flag ) {
      mainColor = c[2];
    } else {
      mainColor = c[3];
    }

    int multipleEntryCount = 1;

    for (String singleDayPathString : multipleDayPath) {

      String[] singleDayPath = singleDayPathString.split(":");

      String currentNodeName = singleDayPath[0];

      for (int i = 1; i < singleDayPath.length; i++) {
        String nextNodeName = singleDayPath[i];

        if (this.getNamedNodes().containsKey(currentNodeName)) {
          Node currentNode = this.getNamedNodes().get(currentNodeName);
          drawNode(currentNode, scale);
          if (this.getNamedNodes().containsKey(nextNodeName)) {
            List<Edge> edgeList = currentNode.getAllEdges(nextNodeName);
            Collections.sort(edgeList);

            int edgeCount = 0;

            setColors(mainColor);

            for (Edge e : edgeList) {
              edgeCount++;
              if (e != null) {
                multipleEntryCount = 1;
                if (edgeCount == 1) drawEdge(e, scale);
                else {
                  drawEdgeDup(e, scale);
                }
              }
              drawNode(this.getNamedNodes().get(nextNodeName), scale);
            }
            if (currentNodeName.equals(nextNodeName)) {
              multipleEntryCount++;

              int x = (currentNode.x + 2) * scale;
              int y = (currentNode.y - 2) * scale;

              parent.text("X" + multipleEntryCount, x, y);
            }
          } else {
            parent.fill(parent.color(255, 0, 0));

            int x = (currentNode.x + 12) * scale;
            int y = (currentNode.y - 2) * scale;
            parent.line(x, y, (x + 4), (y + 4));
            parent.line(x, (y + 4), (x + 4), y);
            parent.text(nextNodeName, x + 12, y + 12);
            if (i < singleDayPath.length - 1) currentNodeName = singleDayPath[i + 1];
            i++;
            continue;
          }
        } else {
          System.out.println("Did not find main.Node: " + currentNodeName);
        }
        currentNodeName = nextNodeName;
      }
    }
  }

  Integer levelMultiplier = 14;

  class NodeNumbers {
    int x, y;
    int topLeftCount, topRightCount;
    int leftUpCount, leftDownCount;
    int rightUpCount, rightDownCount;

    public NodeNumbers(int x, int y) {
      this.x = x;
      this.y = y;
      topLeftCount = topRightCount = 0;
      leftUpCount = leftDownCount = 0;
      rightUpCount = rightDownCount = 0;
    }

    public int getMaxCount () {
      int maxCount = 0;
      if (maxCount < topLeftCount) maxCount = topLeftCount;
      if (maxCount < topRightCount) maxCount = topRightCount;
      if (maxCount < leftUpCount) maxCount = leftUpCount;
      if (maxCount < leftDownCount) maxCount = leftDownCount;
      if (maxCount < rightUpCount) maxCount = rightUpCount;
      if (maxCount < rightDownCount) maxCount = rightDownCount;

      return maxCount;
    }
  }

  class Tuple {
    String _1;
    String _2;

    public Tuple(String a, String b) {
      this._1 = a;
      this._2 = b;
    }
  }

  //  private List<Tuple> straightPairs = new LinkedList<Tuple>(Arrays.asList(new Tuple("entrance1", "general-gate7")));
  private Map<String, String> straightPairs = new HashMap<String, String>() {
    {
      put("entrance1", "general-gate7");
    }
  };

  private boolean isStraightPair(String s, String t) {
    if (straightPairs.containsKey(s) && straightPairs.get(s).equals(t)) return true;
    else if (straightPairs.containsKey(t) && straightPairs.get(t).equals(s)) return true;
    else return false;
  }

  Map<String, NodeNumbers> gateLevels = new HashMap<String, NodeNumbers>(){
    {
//      put("entrance1", new NodeNumbers(1, 5));
      put("entrance1", new NodeNumbers(2, 4));
      put("gate2", new NodeNumbers(2, 3));
      put("ranger-stop4", new NodeNumbers(1, 1));
      put("camping5", new NodeNumbers(1, 7));
      put("ranger-stop1", new NodeNumbers(1, 1));

      put("camping2", new NodeNumbers(2, 2));

//      put("entrance0", new NodeNumbers(3, 1));
//      put("entrance0", new NodeNumbers(4, 1));
      put("entrance0", new NodeNumbers(5, 1));
      put("gate0", new NodeNumbers(3, 3));
      put("gate1", new NodeNumbers(3, 3));
      put("camping0", new NodeNumbers(3, 3));
//      put("general-gate7", new NodeNumbers(3, 10));
      put("general-gate7", new NodeNumbers(3, 9));
      put("general-gate0", new NodeNumbers(3, 3));
//      put("general-gate1", new NodeNumbers(3, 2));
//      put("general-gate1", new NodeNumbers(5, 2));
      put("general-gate1", new NodeNumbers(6, 2));
      put("camping3", new NodeNumbers(3, 3));
//      put("camping4", new NodeNumbers(3, 3));
//      put("camping4", new NodeNumbers(5, 3));
      put("camping4", new NodeNumbers(3, 5));

      put("gate7", new NodeNumbers(4, 4));
      put("ranger-stop7", new NodeNumbers(4, 4));
//      put("ranger-stop2", new NodeNumbers(4, 2));
//      put("ranger-stop2", new NodeNumbers(6, 2));
      put("ranger-stop2", new NodeNumbers(7, 2));
//      put("general-gate4", new NodeNumbers(4, 7));
//      put("general-gate4", new NodeNumbers(4, 6));
      put("general-gate4", new NodeNumbers(5, 6));

      put("gate6", new NodeNumbers(6, 9));
//      put("entrance3", new NodeNumbers(5, 11));
//      put("entrance3", new NodeNumbers(5, 10));
      put("entrance3", new NodeNumbers(6, 10));

      put("ranger-stop6", new NodeNumbers(7, 9));
//      put("ranger-stop0", new NodeNumbers(6, 2));
//      put("ranger-stop0", new NodeNumbers(7, 2));
      put("ranger-stop0", new NodeNumbers(8, 2));

      put("gate5", new NodeNumbers(8, 9));
      put("ranger-base", new NodeNumbers(7, 7));
//      put("general-gate2", new NodeNumbers(7, 2));
//      put("general-gate2", new NodeNumbers(8, 2));
      put("general-gate2", new NodeNumbers(9, 2));

      put("gate8", new NodeNumbers(8, 8));
//      put("entrance4", new NodeNumbers(8, 13));
//      put("entrance4", new NodeNumbers(9, 12));
//      put("entrance4", new NodeNumbers(9, 11));
      put("entrance4", new NodeNumbers(10, 11));
//      put("general-gate5", new NodeNumbers(8, 8));
//      put("general-gate5", new NodeNumbers(8, 7));
      put("general-gate5", new NodeNumbers(9, 7));
      put("general-gate6", new NodeNumbers(8, 8));
      put("camping1", new NodeNumbers(8, 8));

      put("camping6", new NodeNumbers(9, 9));

      put("gate3", new NodeNumbers(9, 4));
      put("ranger-stop3", new NodeNumbers(9, 3));
      put("ranger-stop5", new NodeNumbers(10, 10));

      put("gate4", new NodeNumbers(11, 11));
//      put("entrance2", new NodeNumbers(11, 6));
      put("entrance2", new NodeNumbers(11, 5));
      put("camping7", new NodeNumbers(11, 11));
      put("camping8", new NodeNumbers(11, 11));
      put("general-gate3", new NodeNumbers(11, 11));

      put("legend", new NodeNumbers(1, 12));
    }
  };

  void mapLevels() {
    /*for (String g : this.namedNodes.keySet()) {
      Scanner scan = new Scanner(System.in);
      System.out.println("Level for " + g + " :");
      int level = scan.nextInt();
      gateLevels.put(g, level);
    }*/
    for (Map.Entry<String, NodeNumbers> g : gateLevels.entrySet()) {
      System.out.println(g.getKey() + ", " + g.getValue().x + " : " + g.getValue().y);
    }
  }

  void drawAllSubwayNodes(int scale) {
    for (Map.Entry<String, Node> g : getNamedNodes().entrySet()) {
      drawSubwayNode(g.getValue(), scale);
    }
  }

  void drawLevelsGrid(int scale) {
    for (Map.Entry<String, NodeNumbers> g : gateLevels.entrySet()) {
      parent.stroke(200, 200, 200);
      parent.line(g.getValue().x * levelMultiplier * scale, 0, g.getValue().x * levelMultiplier * scale, 200 * scale);
      parent.line(0, g.getValue().y * levelMultiplier * scale, 200 * scale, g.getValue().y * levelMultiplier * scale);
    }
  }


  String SUBWAY_COLOR = "";

  void drawSubwayMap(String color, JSONArray pathNodes, int scale) {
//    drawLevelsGrid(scale);
    SUBWAY_COLOR = color;

    JSONObject currentNodeObj = pathNodes.getJSONObject(0);
//    parent.stroke(parent.random(255), parent.random(255), parent.random(255));

    for(int i = 1; i < pathNodes.size(); i++) {
      String currentNodeName = currentNodeObj.getString("gate");

      JSONObject nextNodeObj = pathNodes.getJSONObject(i);
      String nextNodeName = nextNodeObj.getString("gate");

      if (this.getNamedNodes().containsKey(currentNodeName) && this.getNamedNodes().containsKey(nextNodeName)) {
        Node currentNode = this.getNamedNodes().get(currentNodeName);
        Node nextNode = this.getNamedNodes().get(nextNodeName);

        parent.strokeWeight(4);

        NodeNumbers source, target;
        if (currentNodeName.hashCode() < nextNodeName.hashCode() ) {
          source = gateLevels.get(currentNodeName);
          target = gateLevels.get(nextNodeName);
        } else {
          source = gateLevels.get(nextNodeName);
          target = gateLevels.get(currentNodeName);
        }

        if ( Math.abs(target.y - source.y) >= Math.abs(target.x - source.x)) {  // go up || down
          if (target.y < source.y) { // go up
            if (source.x == target.x) {
              drawStraightUp(scale, source, target);
            } else if(target.x < source.x) drawUpThenLeft(scale, source, target);
            else drawUpThenRight(scale, source, target);
          } else {  // go down
            if (source.x == target.x) {
              drawStraightDown(scale, source, target);
            } else if(source.x < target.x) drawLeftThenUp(scale, target, source);
            else drawUpThenRight(scale, target, source);
          }
        } else {
          if (target.x < source.x) { // go left
            if (source.y == target.y) {
              drawStraightLeft(scale, source, target);
            } else if(target.y < source.y) drawLeftThenUp(scale, source, target);
            else drawLeftThenDown(scale, target, source);
          } else {  // go right
            if (source.y == target.y) {
              drawStraightRight(scale, source, target);
            } else if(source.y < target.y) drawLeftThenUp(scale, target, source);
            else drawLeftThenDown(scale, target, source);
          }
        }
        parent.strokeWeight(1);

        drawSubwayNode(currentNode, scale);
        drawSubwayNode(nextNode, scale);
      } else {
        System.out.println("Did not find one of the main.Node: " + currentNodeName + " || " + nextNodeName);
      }
      currentNodeObj = nextNodeObj;
    }
  }

  void drawSubwayMap(String color, String[] multipleDayPath, int scale) {
//    drawLevelsGrid(scale);
    SUBWAY_COLOR = color;
    String currentNodeName = "";

    for (String singleDayPathString : multipleDayPath) {
      parent.println("SingleDay path string : " + singleDayPathString);

      String[] singleDayPath = singleDayPathString.split(":");

//      String currentNodeName = singleDayPath[0];
      System.out.println(currentNodeName);
      System.out.println(singleDayPath[0]);

      if (currentNodeName.equals(singleDayPath[0])) {
        currentNodeName = singleDayPath[1];
      } else {
        currentNodeName = singleDayPath[0];
      }
//    parent.stroke(parent.random(255), parent.random(255), parent.random(255));

      for(int i = 1; i < singleDayPath.length; i++) {
        String nextNodeName = singleDayPath[i];

        if (currentNodeName.equals(nextNodeName)) continue;

        if (this.getNamedNodes().containsKey(currentNodeName) && this.getNamedNodes().containsKey(nextNodeName)) {
          Node currentNode = this.getNamedNodes().get(currentNodeName);
          Node nextNode = this.getNamedNodes().get(nextNodeName);

          parent.strokeWeight(4);

          NodeNumbers source, target;
          if (currentNodeName.hashCode() < nextNodeName.hashCode() ) {
            source = gateLevels.get(currentNodeName);
            target = gateLevels.get(nextNodeName);
          } else {
            source = gateLevels.get(nextNodeName);
            target = gateLevels.get(currentNodeName);
          }

          if (isStraightPair(currentNodeName, nextNodeName)) {
            System.out.println("straight diagonal");

            int x1, y1, x2, y2;

            if (source.y < target.y) {
              x1 = (source.x * levelMultiplier - source.rightDownCount++ ) * scale;
              y1 = (source.y * levelMultiplier + source.leftDownCount)* scale;

//              x2 = (target.x * levelMultiplier - target.topLeftCount ) * scale;
//              x2 = (target.x * levelMultiplier) * scale;
              x2 = (target.x * levelMultiplier - target.topLeftCount++ ) * scale;
              y2 = (target.y * levelMultiplier - target.leftUpCount++) * scale;

              drawStraightLine(x1, y1, x2, y2);
              drawEllipse(parent.color(250, 250, 250), x1, y1);
              drawEllipse(parent.color(250, 250, 250), x2, y2);
            }
          } else if ( Math.abs(target.y - source.y) >= Math.abs(target.x - source.x)) {  // go up || down
            if (target.y < source.y) { // go up
              if (source.x == target.x) {
                drawStraightUp(scale, source, target);
              } else if(target.x < source.x) drawUpThenLeft(scale, source, target);
              else drawUpThenRight(scale, source, target);
            } else {  // go down
              if (source.x == target.x) {
                drawStraightDown(scale, source, target);
              } else if(source.x < target.x) drawLeftThenUp(scale, target, source);
              else drawUpThenRight(scale, target, source);
            }
          } else {
            if (target.x < source.x) { // go left
              if (source.y == target.y) {
                drawStraightLeft(scale, source, target);
              } else if(target.y < source.y) drawLeftThenUp(scale, source, target);
              else drawLeftThenDown(scale, target, source);
            } else {  // go right
              if (source.y == target.y) {
                drawStraightRight(scale, source, target);
              } else if(source.y < target.y) drawLeftThenUp(scale, target, source);
              else drawLeftThenDown(scale, target, source);
            }
          }
          parent.strokeWeight(1);

          drawSubwayNode(currentNode, scale);
          drawSubwayNode(nextNode, scale);
        } else {
          System.out.println("Did not find one of the main.Node: " + currentNodeName + " || " + nextNodeName);
        }
        currentNodeName = nextNodeName;
      }
    }
  }

  public void markNodes(int scale) {
    parent.println("gatestolabel: " + gatesToLabel.size());
    for (Node n : gatesToLabel) {
      NodeNumbers nodeInfo = gateLevels.get(n.getLabel());
//      gateLevels.get(node.getLabel()).x * levelMultiplier) * scale
      parent.noFill();
      parent.stroke(n.getNodeColor());
      parent.println("Drawing gate: " + n.getLabel());
      /*int x1 = (nodeInfo.x) * levelMultiplier - (nodeInfo.topLeftCount > nodeInfo.leftDownCount ? nodeInfo.topLeftCount : nodeInfo.leftDownCount );
      int y1 = (nodeInfo.y) * levelMultiplier - (nodeInfo.leftUpCount > nodeInfo.rightUpCount ? nodeInfo.leftUpCount : nodeInfo.rightUpCount );
*/
      int maxOffset = nodeInfo.getMaxCount();

//      int x1 = (nodeInfo.x) * levelMultiplier - maxOffset ;
//      int y1 = (nodeInfo.y) * levelMultiplier - maxOffset;

      int x_offset = (nodeInfo.leftDownCount > nodeInfo.leftUpCount ? nodeInfo.leftDownCount : nodeInfo.leftUpCount);
      int y_offset = (nodeInfo.leftDownCount > nodeInfo.rightDownCount ? nodeInfo.leftDownCount : nodeInfo.rightDownCount);

      int x1 = (nodeInfo.x) * levelMultiplier - x_offset > 0 ? x_offset : 4;
      int y1 = (nodeInfo.y) * levelMultiplier - y_offset > 0 ? y_offset : 4;

//      int width = (nodeInfo.topRightCount > nodeInfo.leftUpCount ? nodeInfo.topRightCount : nodeInfo.leftUpCount );
//      int height = (nodeInfo.leftDownCount > nodeInfo.rightDownCount ? nodeInfo.leftDownCount : nodeInfo.rightDownCount );

      int width = (nodeInfo.topRightCount > nodeInfo.rightDownCount ? nodeInfo.topRightCount : nodeInfo.rightDownCount );
      int height = (nodeInfo.leftDownCount > nodeInfo.rightDownCount ? nodeInfo.leftDownCount : nodeInfo.rightDownCount );

      width = width > 0 ? width : 1;
      height = height > 0 ? height : 1;


      parent.rect(x1 * scale, y1 * scale, x_offset * 10, y_offset * 10, 7);
//      parent.rect(x1 * scale - 8, y1 * scale-8, 15, 15, 5);
    }
  }

  private void setStrokeColor() {
    String[] split = SUBWAY_COLOR.split(",");
    int r = Integer.parseInt(split[0]);
    int g = Integer.parseInt(split[1]);
    int b = Integer.parseInt(split[2]);

    parent.stroke(r, g, b);
  }

  private void drawStraightLine(int x1, int y1, int x2, int y2) {
    parent.stroke(255);
    parent.strokeWeight(4);
    parent.line(x1, y1, x2, y2);

    setStrokeColor();
    parent.strokeWeight(3);
    parent.line(x1, y1, x2, y2);
  }

  private void drawSubwayLine(float x1, float y1, float x2, float y2, float x3, float y3) {
    parent.beginShape();
    parent.stroke(255);
    parent.strokeWeight(4);
    parent.noFill();
    parent.vertex(x1, y1);
    parent.vertex(x2, y2);
    parent.vertex(x3, y3);
    parent.endShape();

    parent.beginShape();
//    parent.stroke(parent.random(255), parent.random(255), parent.random(255));
    setStrokeColor();
    parent.strokeWeight(3);
    parent.noFill();
    parent.vertex(x1, y1);
    parent.vertex(x2, y2);
    parent.vertex(x3, y3);
    parent.endShape();

    drawEllipse(parent.color(250, 250, 250), x1, y1);
    drawEllipse(parent.color(250, 250, 250), x3, y3);
  }

  // draw the new line at the bottom
  private void drawStraightRight(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("straight right");
    int x1 = (source.x * levelMultiplier ) * scale;
    int y1 = (source.y * levelMultiplier + source.rightDownCount++ )* scale;

    int x2 = (target.x * levelMultiplier ) * scale;
    int y2 = y1;

    drawStraightLine(x1, y1, x2, y2);

    drawEllipse(parent.color(250, 250, 250), x1, y1);
    drawEllipse(parent.color(250, 250, 250), x2, y2);
  }

  // draw the new line at the bottom
  private void drawStraightLeft(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("straight left");
    int x1 = (source.x * levelMultiplier ) * scale;
    int y1 = (source.y * levelMultiplier + source.leftDownCount++ )* scale;

    int x2 = (target.x * levelMultiplier ) * scale;
    int y2 = y1;

    drawStraightLine(x1, y1, x2, y2);

    drawEllipse(parent.color(250, 250, 250), x1, y1);
    drawEllipse(parent.color(250, 250, 250), x2, y2);
  }

  // draw the new line on the left
  private void drawStraightDown(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("straight down");
//    int x1 = (source.x * levelMultiplier - source.leftDownCount++) * scale;
    int x1 = (source.x * levelMultiplier - (source.leftDownCount > target.topLeftCount ? source.leftDownCount : target.topLeftCount)) * scale;

    int y1 = source.y * levelMultiplier * scale;
//    int y1 = (source.y * levelMultiplier + source.leftDownCount )* scale;

    if (source.topRightCount == 0) source.topRightCount++;
    if (source.rightDownCount== 0) source.rightDownCount++;
    if (target.topRightCount == 0) target.topRightCount++;


    int x2 = x1;
    int y2 = (target.y * levelMultiplier - target.topLeftCount) * scale;

    source.leftDownCount++;
    target.topLeftCount++;

    drawStraightLine(x1, y1, x2, y2);
    drawEllipse(parent.color(250, 250, 250), x1, y1);
    drawEllipse(parent.color(250, 250, 250), x2, y2);
  }

  // draw the new line on the left
  private void drawStraightUp(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("straight up");
//    int x1 = (source.x * levelMultiplier - (source.topLeftCount > target.leftDownCount ? source.topLeftCount : target.leftDownCount )) * scale;
    int x1 = (source.x * levelMultiplier - (source.topLeftCount > target.leftDownCount ? source.topLeftCount : target.leftDownCount )) * scale;


    int y1 = source.y * levelMultiplier * scale;

    if (source.topRightCount == 0) source.topRightCount++;
    if (target.rightDownCount== 0) target.rightDownCount++;

    int x2 = x1;
    int y2 = (target.y * levelMultiplier + ((target.leftDownCount > target.rightDownCount) ? target.leftDownCount : target.rightDownCount)) * scale;

    source.topLeftCount++;
    target.leftDownCount++;

    drawStraightLine(x1, y1, x2, y2);
    drawEllipse(parent.color(250, 250, 250), x1, y1);
    drawEllipse(parent.color(250, 250, 250), x2, y2);
  }

  // draw new line on the left and above.
  private void drawUpThenLeft(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("Going up then left ");
    float x1 = (source.x * levelMultiplier - source.topLeftCount++) * scale;
    float y1 = (source.y * levelMultiplier - (source.leftUpCount > source.rightUpCount? source.leftUpCount++ : source.rightUpCount++))* scale;

    float x2 = x1;
    float y2 = (target.y * levelMultiplier + target.rightDownCount++) * scale;

//    float x3 = (target.x * levelMultiplier) * scale;
    float x3 = (target.x * levelMultiplier + target.topRightCount++) * scale;
    float y3 = y2;

    drawSubwayLine(x1, y1, x2, y2, x3, y3);
  }

  // draw new line on the right
  private void drawUpThenRight(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("Going up then right");
//        int x1 = (source.x * levelMultiplier - (source.topLeftCount > target.leftDownCount ? source.topLeftCount : target.leftDownCount )) * scale;

    float x1 = (source.x * levelMultiplier + source.topRightCount++) * scale;
    float y1 = source.y * levelMultiplier * scale;
//    target.leftDownCount++;
    if(source.topLeftCount == 0) source.topLeftCount++;

    target.leftUpCount++;
//    if(target.leftDownCount == 0) target.leftDownCount++;

    float x2 = x1;
    float y2 = (target.y * levelMultiplier + target.leftDownCount++) * scale;

    float x3 = (target.x * levelMultiplier) * scale;
    float y3 = y2;

    drawSubwayLine(x1, y1, x2, y2, x3, y3);
  }

  private void drawLeftThenUp(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("Going left then up");

//    float x1 = (source.x * levelMultiplier) * scale;
    float x1 = (source.x * levelMultiplier - source.topLeftCount) * scale;
    float y1 = (source.y * levelMultiplier - source.leftUpCount++)* scale;

    float x2 = (target.x * levelMultiplier + target.rightDownCount++) * scale;
    float y2 = y1;

    float x3 = x2;
//    float y3 = (target.y* levelMultiplier + target.bottomCount++) * scale;
    float y3 = (target.y * levelMultiplier ) * scale;

    drawSubwayLine(x1, y1, x2, y2, x3, y3);
  }

  private void drawLeftThenDown(int scale, NodeNumbers source, NodeNumbers target) {
    System.out.println("Going left then down");

    float x1 = (source.x * levelMultiplier) * scale;
    float y1 = (source.y * levelMultiplier + source.leftDownCount++) * scale;

    float x2 = (target.x * levelMultiplier + target.rightUpCount++) * scale;
    float y2 = y1;

    float x3 = x2;
//    float y3 = (target.y* levelMultiplier + target.bottomCount++) * scale;
    float y3 = (target.y * levelMultiplier ) * scale;

    drawSubwayLine(x1, y1, x2, y2, x3, y3);
  }

  private void drawNode(Node node, int scale) {
    parent.println("drawing main.Node: " + node.getLabel());
    parent.fill(node.getNodeColor());
    parent.ellipse(node.x * scale, node.y * scale, 8, 8);
    /*if(node.getLabel() != null){
      parent.text(node.getLabel(), node.x * scale + 6, node.y * scale + 6);
    }*/
  }

  private void drawSubwayNode(Node node, int scale) {
    parent.println("drawing main.Node: " + node.getLabel());
    drawEllipse(node.getNodeColor(),
        (gateLevels.get(node.getLabel()).x * levelMultiplier) * scale,
        (gateLevels.get(node.getLabel()).y * levelMultiplier) * scale);
    if (!gatesToLabel.contains(node)) {
      gatesToLabel.add(node);
//      labelNode(node, scale);
    }
  }

  private void labelNode(Node node, int scale) {
    if(node.getLabel()!= null){
      parent.stroke(node.getNodeColor());
      parent.text(node.getLabel(),
          (gateLevels.get(node.getLabel()).x * levelMultiplier) * scale + 6,
          (gateLevels.get(node.getLabel()).y * levelMultiplier) * scale - 12);
    }
  }

  private void drawEllipse(int color, float a, float b) {
    parent.fill(color);
    parent.ellipse(a, b, 8,8);
  }

  private void drawEdge(Edge e, int scale) {
    if(e.path.isEmpty()){
      parent.line(e.source.x * scale, e.source.y * scale, e.target.x * scale, e.target.y * scale);
    } else {
      for(Integer i : e.path) {
        int x = i % this.width;
        int y = i / this.width;
        parent.ellipse(x * scale, y * scale, 2, 2);
      }
    }
  }

  private void drawEdge(Edge e, int scale, int c) {
    parent.fill(c);
    parent.stroke(c);
//    setStrokeColor();
    drawEdge(e, scale);
  }

  private void drawEdgeDup(Edge e, int scale) {
    if(e.path.isEmpty()){
      parent.line(e.source.x * scale, e.source.y * scale, e.target.x * scale, e.target.y * scale);
    } else {
      for(Integer i : e.path) {
        int x = i % this.width;
        int y = i / this.width;
        parent.ellipse(x * scale, y * scale, 0.1f, 0.1f);
      }
    }
  }

  private void drawEdgeDup(Edge e, int scale, int c) {
    parent.fill(c);
    parent.stroke(c);
    drawEdgeDup(e, scale);
  }

  /** Returns adjacency-list representation of graph */
  public String toString() {
    String s = "";

    System.out.println("Size: " + this.getNamedNodes().size());

    for (Map.Entry<String, Node> n : this.getNamedNodes().entrySet()){
      Node node = n.getValue();
      s += node.getLabel() + " (" + node.getNeighbours().size() + ")" + " -> ";
      for (Edge e: node.getNeighbours()){
        if(e.target == null){ parent.println("target null"); break; }
        s += e.target.getLabel();
        s += " (" + e.pixelDistance + "), ";
      }
      s += "\n";
    }
    return s;
  }

  /** Writes csv representation: source,target,distance,pixelDistance,sx,sy,tx,ty */
  public void writeEdgeInfoCSV(String path) {
    String s = "source,target,distance,pixelDistance,sx,sy,tx,ty\n";
    for (Map.Entry<String, Node> n : this.getNamedNodes().entrySet()){
      Node node = n.getValue();
      for (Edge e: node.getNeighbours()){
        if(e.target == null){ s += ",,,,,,,,\n"; break; }
        s += node.getLabel();
        s += "," + e.target.getLabel();
        s += "," + e.distance;
        s += "," + e.pixelDistance;
        s += "," + e.source.x;
        s += "," + e.source.y;
        s += "," + e.target.x;
        s += "," + e.target.y;
        s += "\n";
      }
    }
    BufferedWriter writer = null;
    try {
      File file = new File(path);

      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file);
      writer = new BufferedWriter(fw);
      writer.write(s);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      try {
        if(writer!=null) writer.close();
      } catch(Exception ex) {
        System.out.println("Error in closing the BufferedWriter" + ex);
      }
    }
  }
}