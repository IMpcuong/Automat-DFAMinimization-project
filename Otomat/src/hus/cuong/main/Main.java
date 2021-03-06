package hus.cuong.main;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import hus.cuong.io.ReadAutomata;
import hus.cuong.io.WriteAutomata;
import hus.cuong.object.*;
import hus.cuong.queue.Queue;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class Main {
    static TransformFunction tffOld;
    static TransformFunction tffmNew;
    static Alphabet alphabet;
    static State initialStatus;
    static String finishStatus;
    static String[] allStatusInString;
    static ArrayList<State> allStatus;
    static Queue queue;
    static WriteAutomata wa;

    public static void main(String[] args) {
        testOneFile();
        draw();
    }

    public static void testOneFile() {
        readAutomata("dfa_input.txt");
        allStatus = new ArrayList<>();
        try {
            process();
            wa = new WriteAutomata("graph_dfa_input.txt");
            wa.write(tffmNew, alphabet);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void testGraph() {
        DirectedGraph<String, Integer> g = new DirectedSparseMultigraph<>();
        g.addVertex("A");
        g.addVertex("B");
        g.addVertex("C");
        // g.addEdge(12, "A", "B", EdgeType.DIRECTED); // This method
        System.out.println(g.findEdge("A", "C"));
        Layout<String, Integer> layout = new CircleLayout(g);
        layout.setSize(new Dimension(300, 300));
        BasicVisualizationServer<String, Integer> vv = new BasicVisualizationServer<String, Integer>(layout);
        vv.setPreferredSize(new Dimension(350, 350));
        // Setup up a new vertex to paint transformer...
        Transformer<String, Paint> vertexPaint = new Transformer<String, Paint>() {
            public Paint transform(String i) {
                if (i.compareTo("A") == 0)
                    return Color.RED;
                return Color.GREEN;
            }
        };
        // Set up a new stroke Transformer for the edges
        float dash[] = {10.0f};
        final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash,
                0.0f);
        Transformer<Integer, Stroke> edgeStrokeTransformer = new Transformer<Integer, Stroke>() {
            public Stroke transform(Integer s) {
                return edgeStroke;
            }
        };

        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
        renderContext(vv);
    }

    public static void draw() {
        DirectedGraph<String, MyLink> g = new DirectedSparseMultigraph<>();

        for (State state : allStatus) {
            g.addVertex(state.toString());
        }
        for (int i = 0; i < allStatus.size(); i++) {
            for (int j = 0; j < allStatus.size(); j++) {
                State b = allStatus.get(i);
                State e = allStatus.get(j);
                String alphabet = tffmNew.getAllAlphabet(b, e);
                if (g.findEdge(b.toString(), e.toString()) == null
                        && alphabet.compareTo("") != 0) {
                    g.addEdge(new MyLink(alphabet), b.toString(), e.toString());
                }
            }
        }

        Layout<String, Integer> layout = new CircleLayout(g);
        layout.setSize(new Dimension(600, 600));
        BasicVisualizationServer<String, Integer> vv = new BasicVisualizationServer<String, Integer>(layout);
        vv.setPreferredSize(new Dimension(350, 350));
        // Setup up a new vertex to paint transformer...
        Transformer<String, Paint> vertexPaint = new Transformer<String, Paint>() {
            public Paint transform(String i) {
                if (i.compareTo("null") == 0)
                    return Color.GRAY;
                if (i.compareTo(initialStatus.toString()) == 0) {
                    return Color.GREEN;
                }
                String[] listEndStatusInString = finishStatus.split(" ");
                for (String endStt : listEndStatusInString) {
                    if (i.contains(endStt)) return Color.ORANGE;
                }
                return Color.CYAN;
            }
        };
        // Set up a new stroke Transformer for the edges
        float dash[] = {10.0f};
        final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash,
                0.0f);

        Transformer<String, Shape> vertexSize = new Transformer<String, Shape>() {
            public Shape transform(String s) {
                Ellipse2D circle = new Ellipse2D.Double(-15, -15, 80, 60);
                // in this case, the vertex is twice as large
                return circle;
            }
        };
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.getRenderContext().setVertexShapeTransformer(vertexSize);
        renderContext(vv);
    }

    private static void renderContext(BasicVisualizationServer<String, Integer> vv) {
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

        JFrame frame = new JFrame("Simple Graph View 2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(vv);
        frame.pack();
        frame.setVisible(true);
    }

    public static void readAutomata(String filePath) {
        ReadAutomata readAutomata = new ReadAutomata(filePath);
        tffOld = readAutomata.getTfOld();
        alphabet = readAutomata.getAlphabet();
        initialStatus = new State(readAutomata.getInitialStatus());
        initialStatus.setInitialStatus();
        finishStatus = readAutomata.getFinishStatus();
    }

    private static State temp;

    public static void process() throws InterruptedException {
        tffmNew = new TransformFunction();
        queue = new Queue();
        queue.enqueue(initialStatus);
        // initialQueue();
        allStatus.add(initialStatus);
        ArrayList<String> alpha = alphabet.getAlphabe();
        while (!queue.isEmpty()) {
            State currentStatus = (State) queue.dequeue();
            ArrayList<String> listCurrentStatus = currentStatus.getListStatus();
            for (String al : alpha) {
                Transform tf = new Transform();
                tf.setBs(currentStatus);
                tf.setAlphabet(al);
                temp = new State();
                for (String curStt : listCurrentStatus) {
                    State target = tffOld.getTransform(curStt, al);
                    combineStatus(target);
                }
                tf.setEs(temp);
                tffmNew.setTransformFunciton(tf);
                if (!checkStatusExist(temp)) {
                    allStatus.add(temp);
                    queue.enqueue(temp);
                }
            }
        }
        tffmNew.setAllStatus(allStatus);
        setNewFinishStatus();
    }

    public static void setNewFinishStatus() {
        for (State state : allStatus) {
            ArrayList<String> listStt = state.getListStatus();
            for (String stt : listStt) {
                if (finishStatus.contains(stt)) {
                    state.setFinishStatus();
                    continue;
                }
            }
        }
    }

    public static void initialQueue() {
        for (String stt : allStatusInString) {
            State st = new State(stt);
            queue.enqueue(st);
        }
    }

    public static void combineStatus(State stt) {
        ArrayList<String> listStt = stt.getListStatus();
        for (String st : listStt) {
            temp.setStatus(st);
        }
    }

    public static void printTransformFunction(TransformFunction tff) {
        System.out.println(tff.toString());
    }

    public static boolean checkStatusExist(State stt) {
        for (State state : allStatus) {
            if (state.compareTo(stt) == 0)
                return true;
        }
        return false;
    }
}
