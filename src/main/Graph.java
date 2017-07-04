package main; /**
 * Created by atuladhar on 6/27/17.
 */
import org.elasticsearch.common.collect.Tuple;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
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

  Graph(PApplet p, PImage img) {
    this.parent = p;
    System.out.println("here: " + p.getClass());
    this.mapImage = img;
    this.width = img.width;
    this.namedNodes = new LinkedHashMap<String, Node>();
    this.sensorCounts = new LinkedHashMap<String, Integer>();
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

  void drawPathFromNewJson(JSONArray pathNodes, int scale) {
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
  }


  void drawPathFromJson(JSONArray pathNodes, int scale) {
    int multipleEntryCount = 1;

    JSONObject currentNodeObj = pathNodes.getJSONObject(pathNodes.size() - 1);  // data is in reverse order. Therefore, looping backwards
    for(int i = pathNodes.size() - 2; i >= 0; i--) {
      String currentNodeName = currentNodeObj.getString("_2");

      JSONObject nextNodeObj = pathNodes.getJSONObject(i);
      String nextNodeName = nextNodeObj.getString("_2");

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
              float speed = ((float)12/200 * e.pixelDistance * 60 * 60) / nextNodeObj.getInt("_3");
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
  }

  Integer levelMultiplier = 14;

  class NodeNumbers {
    int x, y;
    int topCount, bottomCount, leftCount, rightCount;

    public NodeNumbers(int x, int y) {
      this.x = x;
      this.y = y;
      topCount = bottomCount = leftCount = rightCount = 0;
    }
  }


  Map<String, NodeNumbers> gateLevels = new HashMap<String, NodeNumbers>(){
    {
      put("entrance1", new NodeNumbers(1, 5));
      put("gate2", new NodeNumbers(1, 1));
      put("ranger-stop4", new NodeNumbers(1, 1));
      put("camping5", new NodeNumbers(1, 1));
      put("ranger-stop1", new NodeNumbers(1, 1));

      put("camping2", new NodeNumbers(2, 2));

      put("entrance0", new NodeNumbers(3, 1));
      put("gate0", new NodeNumbers(3, 3));
      put("gate1", new NodeNumbers(3, 3));
      put("camping0", new NodeNumbers(3, 3));
      put("general-gate7", new NodeNumbers(3, 10));
      put("general-gate0", new NodeNumbers(3, 3));
      put("general-gate1", new NodeNumbers(3, 2));
      put("camping3", new NodeNumbers(3, 3));
      put("camping4", new NodeNumbers(3, 3));

      put("gate7", new NodeNumbers(4, 4));
      put("ranger-stop7", new NodeNumbers(4, 4));
      put("ranger-stop2", new NodeNumbers(4, 2));
      put("general-gate4", new NodeNumbers(4, 7));

      put("gate6", new NodeNumbers(5, 5));
      put("entrance3", new NodeNumbers(5, 11));

      put("ranger-stop6", new NodeNumbers(6, 6));
      put("ranger-stop0", new NodeNumbers(6, 2));

      put("gate5", new NodeNumbers(7, 7));
      put("ranger-base", new NodeNumbers(7, 7));
      put("general-gate2", new NodeNumbers(7, 2));

      put("gate8", new NodeNumbers(8, 8));
      put("entrance4", new NodeNumbers(8, 13));
      put("general-gate5", new NodeNumbers(8, 8));
      put("general-gate6", new NodeNumbers(8, 8));
      put("camping1", new NodeNumbers(8, 8));

      put("camping6", new NodeNumbers(9, 9));

      put("gate3", new NodeNumbers(10, 10));
      put("ranger-stop3", new NodeNumbers(10, 10));
      put("ranger-stop5", new NodeNumbers(10, 10));

      put("gate4", new NodeNumbers(11, 11));
      put("entrance2", new NodeNumbers(11, 6));
      put("camping7", new NodeNumbers(11, 11));
      put("camping8", new NodeNumbers(11, 11));
      put("general-gate3", new NodeNumbers(11, 11));
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

  void drawLevelsGrid(int scale) {
    for (Map.Entry<String, NodeNumbers> g : gateLevels.entrySet()) {
      parent.stroke(200, 200, 200);
      parent.line(g.getValue().x * levelMultiplier * scale, 0, g.getValue().x * levelMultiplier * scale, 200 * scale);
      parent.line(0, g.getValue().y * levelMultiplier * scale, 200 * scale, g.getValue().y * levelMultiplier * scale);
    }
  }

  void drawSubwayMap(JSONArray pathNodes, int scale) {

//    JSONObject currentNodeObj = pathNodes.getJSONObject(pathNodes.size() - 1);  // data is in reverse order. Therefore, looping backwards
//    for(int i = pathNodes.size() - 2; i >= 0; i--) {
    drawLevelsGrid(scale);

    JSONObject currentNodeObj = pathNodes.getJSONObject(0);

    for(int i = 1; i < pathNodes.size(); i++) {
      String currentNodeName = currentNodeObj.getString("gate");

      JSONObject nextNodeObj = pathNodes.getJSONObject(i);
      String nextNodeName = nextNodeObj.getString("gate");

      if (this.getNamedNodes().containsKey(currentNodeName)) {
        Node currentNode = this.getNamedNodes().get(currentNodeName);
        drawSubwayNode(currentNode, scale);
        if (this.getNamedNodes().containsKey(nextNodeName)) {
          Node nextNode = this.getNamedNodes().get(nextNodeName);
          drawSubwayNode(nextNode, scale);
          parent.strokeWeight(4);
          parent.stroke(0, 120, 0);
//          parent.line(gateLevels.get(currentNodeName).v1() * levelMultiplier * scale, currentNode.y * scale, gateLevels.get(nextNodeName).v1() * levelMultiplier * scale, nextNode.y * scale);

          NodeNumbers source = gateLevels.get(currentNodeName);

          NodeNumbers target = gateLevels.get(nextNodeName);

//          PVector vertical = new PVector(source.x, source.y);
//          PVector horizontal = new PVector(target.x, target.y);


          if ( Math.abs(target.y - source.y) >= Math.abs(target.x - source.x)) {  // go up / down
//          if ( (target.y - source.y) > (target.x - target.y)) {  // go up / down
            if (target.y < source.y) { // go up
              System.out.println("Going up ");
              float x1 = (source.x * levelMultiplier + source.topCount++) * scale;
              float y1 = source.y * levelMultiplier * scale;
//              float x2 = (source.x* levelMultiplier + source.topCount) * scale;
              float x2 = x1;
              float y2 = (target.y * levelMultiplier + target.bottomCount++) * scale;
              float x3 = (target.x* levelMultiplier) * scale;
//              float y3 = (target.y* levelMultiplier + target.bottomCount) * scale;
              float y3 = y2;

              parent.beginShape();
              parent.noFill();
              parent.vertex(x1, y1);
              parent.vertex(x2, y2);
              parent.vertex(x3, y3);
              parent.endShape();
            } else {  // go down
              System.out.println("Going down ");
              float x1 = (source.x* levelMultiplier + source.bottomCount++) * scale;
              float y1 = source.y* levelMultiplier * scale;
//              float x2 = (source.x* levelMultiplier + source.topCount) * scale;
              float x2 = x1;
              float y2 = (target.y* levelMultiplier + target.bottomCount++) * scale;
              float x3 = (target.x* levelMultiplier) * scale;
//              float y3 = (target.y* levelMultiplier + target.bottomCount) * scale;
              float y3 = y2;

              parent.beginShape();
              parent.noFill();
              parent.vertex(x1, y1);
//              parent.curveVertex(x2, y2);
              parent.vertex(x2, y2);
              parent.vertex(x3, y3);
              parent.endShape();
            }

          } else {
            System.out.println("Going else ");
            if (target.x < source.x) { // go left
              float x1 = (source.x * levelMultiplier + source.leftCount++) * scale;
              float y1 = source.y * levelMultiplier * scale;
              float x2 = (target.x * levelMultiplier + source.rightCount++) * scale;
              float y2 = y1;
//              float x3 = (target.x* levelMultiplier) * scale;
              float x3 = x2;
              float y3 = (target.y* levelMultiplier + target.bottomCount++) * scale;

              parent.beginShape();
              parent.noFill();
              parent.vertex(x1, y1);
              parent.vertex(x2, y2);
              parent.vertex(x3, y3);
              parent.endShape();
            }


          }

          /*parent.line( x1* levelMultiplier * scale,
              y1 * levelMultiplier * scale,
              x2 * levelMultiplier * scale,
              y2 * levelMultiplier * scale);*/


          parent.strokeWeight(1);
        } else {
          System.out.println("Going outerelse ");
          parent.fill(parent.color(255, 0, 0));
          int x = (gateLevels.get(currentNodeName).x * levelMultiplier) * scale;
          int y = (currentNode.y + 5) * scale;
          parent.line(x , y, (x + 4), (y + 4));
          parent.line(x , (y + 4), (x + 4), y);
          parent.text(nextNodeName, x + 12, y + 12);
        }
      } else {
        System.out.println("Did not find main.Node: " + currentNodeName);
      }
      currentNodeObj = nextNodeObj;
    }
  }

  private void drawNode(Node node, int scale) {
    parent.println("drawing main.Node: " + node.getLabel());
    parent.fill(node.getNodeColor());
    parent.ellipse(node.x * scale, node.y * scale, 5, 5);
    if(node.getLabel() != null){
      parent.text(node.getLabel(), node.x * scale + 6, node.y * scale + 6);
    }
  }

  private void drawSubwayNode(Node node, int scale) {
    parent.println("drawing main.Node: " + node.getLabel());
    parent.fill(node.getNodeColor());
//    parent.ellipse((gateLevels.get(node.getLabel()).v1() * levelMultiplier) * scale, node.y * scale, 8, 8);
    parent.ellipse(
        (gateLevels.get(node.getLabel()).x * levelMultiplier) * scale,
        (gateLevels.get(node.getLabel()).y * levelMultiplier) * scale,
        8,
        8);
    if(node.getLabel() != null){
//      parent.text(node.getLabel(), (gateLevels.get(node.getLabel()).v1() * levelMultiplier) * scale + 6, node.y * scale + 6);
      parent.text(node.getLabel(),
          (gateLevels.get(node.getLabel()).x * levelMultiplier) * scale + 6,
          (gateLevels.get(node.getLabel()).y * levelMultiplier) * scale + 6);
    }
  }

  private void drawEdge(Edge e, int scale) {
    drawEdge(e, scale, parent.color(0,0,0));
  }

  private void drawEdge(Edge e, int scale, int c) {
    parent.fill(c);
    parent.stroke(c);
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

  private void drawEdgeDup(Edge e, int scale, int c) {
    parent.fill(c);
    parent.stroke(c);
    if(e.path.isEmpty()){
      parent.line(e.source.x * scale, e.source.y * scale, e.target.x * scale, e.target.y * scale);
    } else {
      for(Integer i : e.path) {
        int x = i % this.width;
        int y = i / this.width;
        parent.ellipse(x * scale, y * scale, 0.2f, 0.2f);
      }
    }
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

  public List<Node> dfs(Node start, Node goal) {
    if (start == null || goal == null) {
      System.out.println("Start or goal node is null!  No path exists.");
      return new LinkedList<Node>();
    }

    HashMap<Node, Node> parentMap = new HashMap<Node, Node>();
    boolean found = dfsSearch(start, goal, parentMap);

    if (!found) {
      System.out.println("No path exists");
      return new LinkedList<Node>();
    }

    // reconstruct the path
    return constructPath(start, goal, parentMap);
  }

  private List<Node> constructPath(Node start, Node goal,
                                   HashMap<Node, Node> parentMap) {
    LinkedList<Node> path = new LinkedList<Node>();
    Node curr = goal;
    while (curr != start) {
      path.addFirst(curr);
      curr = parentMap.get(curr);
    }
    path.addFirst(start);
    return path;
  }

  private boolean dfsSearch(Node start, Node goal,
                            HashMap<Node, Node> parentMap)
  {
    HashSet<Node> visited = new HashSet<Node>();
    Stack<Node> toExplore = new Stack<Node>();
    toExplore.push(start);
    boolean found = false;

    // Do the search
    while (!toExplore.empty()) {
      Node curr = toExplore.pop();
      if(curr.getLabel() != null ) {

      }
      List<Edge> neighbors = curr.getNeighbours();
      ListIterator<Edge> it = neighbors.listIterator(neighbors.size());
      while (it.hasPrevious()) {
        Node next = getNamedNodes().get(it.previous().endPixel);
        if (!visited.contains(next)) {
          visited.add(next);
          parentMap.put(next, curr);
          toExplore.push(next);
        }
      }
    }
    return found;
  }
}