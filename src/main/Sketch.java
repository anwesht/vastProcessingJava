package main; /**
 * Created by atuladhar on 6/27/17.
 */
import org.elasticsearch.search.SearchHit;
import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.net.UnknownHostException;
import java.util.*;

public class Sketch extends PApplet {

  private PImage myImage;
  private final int GENERAL_GATES = color(0.0f, 255.0f, 255.0f);  // blue
  private final int ENTRANCE = color(76.0f, 255.0f, 0.0f);        // green
  private final int RANGER_STOPS = color(255.0f, 216.0f, 0.0f);   // yellow
  private final int CAMPING = color(255.0f, 106.0f, 0.0f);        // orange
  private final int GATES = color(255.0f, 0.0f, 0.0f);            // red
  private final int RANGER_BASE = this.color(255.0f, 0.0f, 220.0f);    // pink

  private final int WHITE = color(255.0f, 255.0f, 255.0f);        // white to check non road pixels.

  private Graph sensor;

  private int scale = 5;
  private boolean DEBUG = false;

  public static void main(String args[]) {
    PApplet.main("main.Sketch");
  }

  @Override
  public void settings() {
    // TODO: Customize screen size and so on here
    size(1000, 1000);      // scaling to get a bigger graph. multiplying in draw graph function.
  }

  void debugPrint(String s) {
    if(DEBUG) println(s);
  }

  void debugPrint(boolean cond, String s) {
    if (cond) { debugPrint(s); }
  }

  @Override
  public void setup() {

    noLoop();

    myImage = loadImage("/Users/atuladhar/projects/vastChallenge/lekagulRoadways_roads_only.png");
    myImage.loadPixels();

    Graph g = createGraph();
    g.findLandMarks();    // find landmark nodes in the graph.

    // Create a minimized graph with only landmarks as nodes. Also store pixel distance.
    sensor = createSensorGraphDFS(g);
    //sensor.writeEdgeInfoCSV("/Users/atuladhar/projects/vastChallenge/processing/sketch_graph/outputs/graphEdgeInfoRenamedCSV.csv");
  }

  private void drawLegend(int count) {

  }

  @Override
  public void draw() {
    //image(myImage, 0, 0);
    //plotSensorColors();

    //Drawing main.Graph
    background(255);
    //g.draw(scale);
    //sensor.draw(scale);

//    JSONObject obj = loadJSONObject("/Users/atuladhar/projects/vastChallenge/processing/sketch_graph/test.json");
//    JSONObject obj = loadJSONObject("data/currentpath.json");

    String[] lines = loadStrings("data/currentpath.json");

    String[] colors = {
        "127,201,127",  // green
        "190,174,212",  // light purple
        "253,192,134",  // orange
        "152,78,163",   // purple
        "56,108,176",   // blue
        "240,2,127"     // pink
    };

    int count = 0;
    for (String l: lines){
      count++;
//      if (count == 6) {
      JSONObject obj = parseJSONObject(l);
      JSONArray timedPath = obj.getJSONArray("timedPath");
      println("\nDrawing path: " + count + " : "+ obj.getString("path"));
//      sensor.drawPathFromNewJson(colors[count-1], timedPath, scale);
      sensor.drawSubwayMap(colors[count-1], timedPath, scale);

      String[] split = colors[count-1].split(",");
      int r = Integer.parseInt(split[0]);
      int g = Integer.parseInt(split[1]);
      int b = Integer.parseInt(split[2]);

      fill(r, g, b);
      stroke(255, 255, 255);

      float x = (sensor.gateLevels.get("legend").x * sensor.levelMultiplier) * scale;
      float y = (sensor.gateLevels.get("legend").y * sensor.levelMultiplier) * scale + count * sensor.levelMultiplier;

      rect(x - sensor.levelMultiplier , y - sensor.levelMultiplier + 6, sensor.levelMultiplier - 2, sensor.levelMultiplier - 2);
      text(obj.getString("path"),
          x + sensor.levelMultiplier + 5,
          y);

//      }
      if (count == 6) break;
    }
//    sensor.markNodes(scale);

//    sensor.drawAllSubwayNodes(scale);

    // Shape Test: Create a rounded edge.
    /*beginShape();
    vertex(10, 10);
    vertex(50, 10);
    curveVertex(50, 10);
    curveVertex(53, 13);
    vertex(53, 13);
    vertex(53, 50);
    endShape();*/

//    sensor.drawPathFromJson(timedPath, scale);
//    sensor.drawPathFromNewJson(timedPath, scale);

//    sensor.mapLevels();

    //Testing main.ES:
//    testES(obj);
    save("outputs/TEST");
  }

  private void testES(JSONObject obj) {
    ES es = new ES();
    try {
      es.insertData(obj);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    for (SearchHit s : es.getData()) {
      System.out.println(s.getSource());
    }
  }

  /** Create graph representation of the map
   * Each pixel on the road is represented as a node in the graph.
   */
  Graph createGraph() {
    System.out.println("here: " + this.getClass());

    Graph graph = new Graph(this, myImage);

    for (int i = 0; i < 200*200; i++) {
      int currentPixel = myImage.pixels[i];

      if(currentPixel != WHITE){
        Node node = new Node(this, i, graph.getWidth(), currentPixel);

        for (Integer n : graph.findNeighbours(i)){
          node.addWeightedNeighbour(new Node(this, n, graph.getWidth(), myImage.pixels[n]), 0);
        }
        graph.addNamedNode(node);
      }
    }
    return graph;
  }

  /** Use the result from createGraph (with landmarks) to create a new minimized graph
   * with only landmarks as nodes. Also calculates the pixel distances between the landmarks
   */
  private Graph createSensorGraphDFS(Graph g){
    Graph sg = new Graph(this, myImage);
    Map<Integer, Integer> distMap = new HashMap<Integer, Integer>();       // to count pixel distance.
    Map<Integer, List<Integer>> pathMap = new HashMap<Integer, List<Integer>>();    // to track the path of the edge.

    int depth = 0;

    for (Map.Entry<String, Node> n : g.namedNodes.entrySet()){
      if(n.getValue().getLabel() != null){
        Node sourceNode = new Node(n.getValue());    // A sensor node
        Node sgNode = new Node(n.getValue());        // Create a new node for the sensor graph
        sgNode.initNeighbours();                     // Initialise neighbours for sensor graph node

        for (Edge sourceNeighbor : sourceNode.getNeighbours()) {  // loop over all immediate neighbours of the sensor node.
          Node node = g.namedNodes.get(sourceNeighbor.target.getName());
          Node prev = node;    // track previous node to remove from the neighbour list and check backtracking.

          distMap.clear();      // start fresh for each immediate neighbour of sensor node.
          distMap.put(node.getPixel(), 1);    // each immediate neighbour of sensor node is 1 pixel away.

          pathMap.clear();      // start fresh for each immediate neighbour of sensor node.
          pathMap.put(node.getPixel(), new LinkedList<Integer>());

          // dfs initialisation
          LinkedList<Node> visited = new LinkedList<Node>();    // Changed from HashSet to LinkedList to remove deeper nodes when backtracking.

          Stack<Node> toExplore = new Stack<Node>();    // Track the current node to explore.
          toExplore.push(node);
          visited.add(node);

          // Do the search
          while (!toExplore.empty()) {
            Node curr = toExplore.pop();

            if(distMap.get(prev.getPixel()) >= distMap.get(curr.getPixel())){
              depth = distMap.get(prev.getPixel()) - distMap.get(curr.getPixel());
              if(!visited.isEmpty()) visited.subList(visited.size() - depth, visited.size()).clear();
            }

            List<Edge> neighbors = curr.getNeighbours();

            //remove parent node
            neighbors.remove(prev);

            ListIterator<Edge> it = neighbors.listIterator(neighbors.size());

            while (it.hasPrevious()) {     //reverse. ???
              Node next = g.namedNodes.get(it.previous().target.getName());

              if(next.getLabel() != null && next.getPixel() != sourceNode.getPixel()) {
                distMap.put(next.getPixel(), distMap.get(curr.getPixel()) + 1);

                List currentPath = new LinkedList<Integer>(pathMap.get(curr.getPixel()));
                pathMap.put(next.getPixel(), currentPath);

                sgNode.addWeightedNeighbour(next, distMap.get(next.getPixel()), pathMap.get(next.getPixel()));
              } else if (!visited.contains(next) && next.getPixel() != sourceNode.getPixel()) {
                distMap.put(next.getPixel(), distMap.get(curr.getPixel()) + 1);

                List currentPath = new LinkedList<Integer>(pathMap.get(curr.getPixel()));
                currentPath.add(next.getPixel());
                pathMap.put(next.getPixel(), currentPath);

                visited.add(next);
                toExplore.push(next);
              }
            }
            prev = curr;
          }
          sg.addNamedNode(sgNode);
        }
      }
    }
    println(sg.toString());
    return sg;
  }

  /** Get the pixel value based on x and y coordinates.
   @param x -> x-coordinate
   @param y -> y-coordinate
   @returns pixel -> pixel value OR returns NULL for negative coordinates
   */
  Integer getPixelValue(Integer x, Integer y, Integer width) {
    if (x < 0 || y < 0) { return null; }
    return (x + y*width);
  }

  /** Map the types of sensors to pixel position */
  Map<Integer, String> findSensorPositions(){
    int ggCount = 0, eCount = 0, rsCount = 0, cCount = 0, gCount = 0;
    //Map<String, Integer> landMarks = new LinkedHashMap<String, Integer>();
    Map<Integer, String> landMarks = new LinkedHashMap<Integer, String>();

    int white = color(255.0f, 255.0f, 255.0f);
    int count = 0;

    for (int i = 0; i < 200*200; i++) {
      int currentPixel = myImage.pixels[i];
      if(currentPixel == GENERAL_GATES) {
        landMarks.put(i, "generalGate"+Integer.toString(ggCount++));
      } else if(currentPixel == ENTRANCE) {
        landMarks.put(i, "entrance"+Integer.toString(eCount++));
      } else if(currentPixel == RANGER_STOPS) {
        landMarks.put(i, "rangerStop"+Integer.toString(rsCount++));
      } else if(currentPixel == CAMPING) {
        landMarks.put(i, "camping"+Integer.toString(cCount++));
      } else if(currentPixel == GATES) {
        landMarks.put(i, "gates"+Integer.toString(gCount++));
      }

    /*else if (currentPixel != white) {
      count++;
      println(count + "pixel number color = " + i + " R = " + red(currentPixel) + " G = " + green(currentPixel) + " B = " + blue(currentPixel));
    }*/
    }
    return landMarks;
  }

  void findSensorColors(){
    float r, g, b;

    for (int i = 0; i < 200*200; i++) {
      r = red(myImage.pixels[i]);
      g = green(myImage.pixels[i]);
      b = blue(myImage.pixels[i]);

      if (r == 255.0 && g == 255.0 && b == 255.0){
        myImage.pixels[i] = color(255,255,255,0);
      } else if ((r == 0.0 && g == 0.0 && b == 0.0) || ( r == g && r == b && b == g )){
        myImage.pixels[i] = color(255,255,255,0);
      } else {
        // Print Sensor colors
        println("pixel number = " + i + " R = " + r + " G = " + g + " B = " + b);
      }
    }
  }

  /** Sensor colors used in the map:
   1. BLUE = GENERAL GATES   => R = 0.0   G = 255.0 B = 255.0
   2. GREEN = ENTRANCE       => R = 76.0  G = 255.0 B = 0.0
   3. YELLOW = RANGER-STOPS  => R = 255.0 G = 216.0 B = 0.0
   4. ORANGE = CAMPING       => R = 255.0 G = 106.0 B = 0.0
   5. RED = GATES            => R = 255.0 G = 0.0   B = 0.0
   */
  void plotSensorColors() {
    background(255);

    int length = 20, pos = 1;
    fill(0, 255, 255);
    rect(length * pos, length, length, length);

    pos += 1;
    fill(76, 255, 0);
    rect(length * pos, length, length, length);

    pos += 1;
    fill(255, 216, 0);
    rect(length * pos, length, length, length);

    pos += 1;
    fill(255, 106, 0);
    rect(length * pos, length, length, length);

    pos += 1;
    fill(255, 0, 0);
    rect(length * pos, length, length, length);
  }
}

