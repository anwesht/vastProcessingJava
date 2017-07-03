package main;

import processing.core.PApplet;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by atuladhar on 6/27/17.
 */
public class Node {
  PApplet parent;
  public int pixel, x, y;
  //private String label = "DEFAULT";
  private String label;
  private List<Edge> neighbours;
  private int nodeColor;
  private int width;

  public Node(PApplet p, int pixel, int width, int c) {
    this.parent = p;
    this.pixel = pixel;
    this.nodeColor = c;
    this.width = width;
    this.x = pixel % width;
    this.y = pixel / width;
    this.label = null;

    initNeighbours();
  }

  public Node(Node copy) {
    this(copy.parent, copy.getPixel(), copy.getWidth(), copy.getNodeColor());
    this.setLabel(copy.getLabel());
    this.neighbours = new LinkedList<Edge>(copy.getNeighbours());
  }

  public void addNeighbour(int n){
    this.neighbours.add(new Edge(this.parent, this.pixel, n));
  }

  public void addNeighbour(int n, int pixelDist){
    this.neighbours.add(new Edge(this.parent, this.pixel, n, pixelDist));
  }

  public void addWeightedNeighbour(Node target, int pixelDist){
    this.neighbours.add(new Edge(this.parent, this, new Node(target), pixelDist));
  }

  public void addWeightedNeighbour(Node target, int pixelDist, List<Integer> path){
    this.neighbours.add(new Edge(this.parent, this, new Node(target), pixelDist, path));
  }

  public void initNeighbours() {
    this.neighbours = new LinkedList<Edge>();
  }

  public List<Edge> getNeighbours(){
    return this.neighbours;
  }

  public Boolean hasNeighbour(String n) {
    Boolean hasNeighbour = false;
    for(Edge e : this.getNeighbours()) {
      if (e.getTarget().getLabel().equals(n)) {
        hasNeighbour = true;
        break;
      }
    }
    return hasNeighbour;
  }

  public Edge getEdge(String target) {
    Edge returnEdge = null;
    for(Edge e : this.getNeighbours()) {
      if (e.getTarget().getLabel().equals(target)) {
        returnEdge = e;
        break;
      }
    }
    return returnEdge;
  }

  public List<Edge> getAllEdges(String target) {
    List<Edge> edgeList = new LinkedList<Edge>();
    int edgeCount = 0;
    System.out.println("source pixel " + this.pixel);
    for(Edge e : this.getNeighbours()) {
      if (e.getTarget().getLabel().equals(target)) {
        edgeCount++;
        System.out.print(this.getLabel() + " -- " + target + " edge count = " + edgeCount);
        System.out.println(" distance = " + e.pixelDistance);
        System.out.println("path = " + e.path);
        Boolean isCopy = false;
        for (Edge innerE : edgeList) {
          if (innerE.path.size() == e.path.size() - 2 || innerE.path.size() == e.path.size() + 2) {
            isCopy = true;
            break;
          }
        }
        if (!isCopy) edgeList.add(e);
        //if (edgeCount == 2) break;   //debug.
      }
    }
    return edgeList;
  }

  public int getPixel(){
    return this.pixel;
  }

  public int getWidth(){
    return this.width;
  }

  public int getNodeColor(){
    return this.nodeColor;
  }

  public void setLabel(String l){
    this.label = l;
  }

  public String getLabel() {
    return this.label;
  }

  public String getName() {
    return this.getLabel() != null ? this.getLabel() : Integer.toString(this.getPixel());
  }

  public String directionTo (Node dest) {
    String direction = "O";

    if(dest.x < this.x && dest.y == this.y) direction = "W";
    else if (dest.x > this.x && dest.y == this.y) direction = "E";
    else if (dest.y < this.y && dest.x == this.x) direction = "N";
    else if (dest.y < this.y && dest.x > this.x) direction = "NE";
    else if (dest.y < this.y && dest.x < this.x) direction = "NW";
    else if (dest.y > this.y && dest.x == this.x) direction = "S";
    else if (dest.y > this.y && dest.x > this.x) direction = "SE";
    else if (dest.y > this.y && dest.x < this.x) direction = "SW";

    return direction;
  }

  @Override
  public String toString() {
    String s = "";
    s += getLabel() + " @ ";
    s += "(" + x + ", " + y + ")";
    s += " pixel = " + getPixel();
    s += " R = " + parent.red(getNodeColor()) + " G = " + parent.green(getNodeColor()) + " B = " + parent.blue(getNodeColor());
    return s;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Node)) return false;
    Node n = (Node) other;
    return (this.getPixel() == n.getPixel()
        && this.x == n.x
        && this.y == n.y
        && this.label == n.label);
  }

  @Override
  public int hashCode(){
    int hash = pixel;
    hash = hash * 31 + x;
    hash = hash * 31 + y;
    hash = hash * 31 + (getLabel() != null ? getLabel().hashCode() : "DEFAULT".hashCode());
    return hash;
  }
}