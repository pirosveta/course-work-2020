import com.jogamp.opengl.*;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.Frame;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

class Vertex {
    private final int x, y;

    public Vertex(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

public class FractalLandscape {
    protected final static float ANGLE_SPEED = 5, MOVE_SPEED = 0.8f, ZOOM_SPEED = 1.05f,
            BACKGROUND_RED = 0.15f, BACKGROUND_GREEN = 0.15f, BACKGROUND_BLUE = 0.15f;
    protected final static double COS_T = Math.cos(Math.toRadians(7.10)), SIN_T = Math.sin(Math.toRadians(7.10)),
            COS_F = Math.cos(Math.toRadians(41.25)), SIN_F = Math.sin(Math.toRadians(41.25)),
            HILL_RED = 0.14, HILL_GREEN = 0.2, HILL_BLUE = 0.12,
            PLAIN_RED = 0.29, PLAIN_GREEN = 0.85, PLAIN_BLUE = 0.07;
    protected final static int SEED = 23456, MAX_HEIGHT = 20, SIZE_FRAME = 80, SIZE_LANDSCAPE = 50;
    protected final static Random RANDOMIZER = new Random(SEED);

    protected static boolean bePressed = false, change = true, color = true, foundation = true, reFill = true;
    protected static int degree = 8, keyCode = 0x00, size, colorFactor = 50;
    protected static float x = 0, y = 0, angle = 0, aX = 0, aY = 0, aZ = 0,  zoom = 1;
    protected static double maxValue, minValue, landSharp = 0.5;
    protected static ArrayList<Vertex> lastVertex = new ArrayList<>();
    protected static ArrayList<ArrayList<Double>> height = new ArrayList<>();

    protected static void diamondComputation(int range, int x, int y) {
        int top = 0, bottom = 1, left = 2, right = 3, vertexNumber = 4;
        int[] vertexCoordinate = {y - range, y + range, x - range, x + range};

        // Checking borders
        for (int i = 0; i < vertexNumber; i++) {
            if (vertexCoordinate[i] < 0 || vertexCoordinate[i] > size) {
                switch (i) {
                    case (0):
                        top = -1;
                        break;
                    case (1):
                        bottom = -1;
                        break;
                    case (2):
                        left = -1;
                        break;
                    case (3):
                        right = -1;
                        break;
                }
                vertexNumber--;
                break;
            }
        }

        // Computation of coordinates of required vertices
        double topHeight = top >= 0 ? height.get(vertexCoordinate[top]).get(x) : 0,
                bottomHeight = bottom >= 0 ? height.get(vertexCoordinate[bottom]).get(x) : 0,
                leftHeight = left >= 0 ? height.get(y).get(vertexCoordinate[left]) : 0,
                rightHeight = right >= 0 ? height.get(y).get(vertexCoordinate[right]) : 0;

        // Computation of middle height
        double newHeight = (leftHeight + rightHeight + topHeight + bottomHeight) / vertexNumber +
                (FractalLandscape.RANDOMIZER.nextInt(MAX_HEIGHT * 2 + 1) - MAX_HEIGHT) *
                        ((double)range / size) * landSharp;
        height.get(y).set(x, newHeight);
    }

    protected static void diamond(int vertexNumber, int range) {
        for (int i = 0; i < vertexNumber; i++) {
            Vertex v = lastVertex.get(0);
            int x = v.getX(), y = v.getY();

            // Computation of coordinates of required vertices
            int topBorder = y - range, bottomBorder = y + range, leftBorder = x - range, rightBorder = x + range;

            // Computation of middle height
            diamondComputation(range, x, topBorder);
            diamondComputation(range, x, bottomBorder);
            diamondComputation(range, leftBorder, y);
            diamondComputation(range, rightBorder, y);

            lastVertex.remove(0);
        }
    }

    protected static void square(int vertexNumber, int range) {
        for (int i = 0; i < vertexNumber; i++) {
            Vertex v = lastVertex.get(i);
            int x = v.getX(), y = v.getY();

            // Computation of coordinates of required vertices
            int topBorder = y - range, bottomBorder = y + range, leftBorder = x - range, rightBorder = x + range;
            double leftTopVertex = height.get(topBorder).get(leftBorder),
                    rightTopVertex = height.get(topBorder).get(rightBorder),
                    leftBottomVertex = height.get(bottomBorder).get(leftBorder),
                    rightBottomVertex = height.get(bottomBorder).get(rightBorder);

            // Computation of middle height
            double newHeight = (leftTopVertex + rightTopVertex + leftBottomVertex + rightBottomVertex) / 4 +
                    (FractalLandscape.RANDOMIZER.nextInt(MAX_HEIGHT * 2 + 1) - MAX_HEIGHT) *
                            ((double)range / size) * landSharp;
            height.get(y).set(x, newHeight);

            // Adding new vertices in ArrayList
            int nextRange = range / 2;
            if (nextRange > 0) {
                lastVertex.add(new Vertex(x - nextRange, y - nextRange));
                lastVertex.add(new Vertex(x + nextRange, y - nextRange));
                lastVertex.add(new Vertex(x - nextRange, y + nextRange));
                lastVertex.add(new Vertex(x + nextRange, y + nextRange));
            }
        }
    }

    protected static void fillHeightArray() {
        // ArrayList declaration
        height.clear();
        size = (int)Math.pow(2, degree);
        for (int i = 0; i <= size; i++) {
            height.add(new ArrayList<>());
            for (int j = 0; j <= size; j++) {
                height.get(i).add(0.0);
            }
        }

        // Setting the heights of the starting vertices
        height.get(0).set(0, (double)RANDOMIZER.nextInt(MAX_HEIGHT + 1));
        height.get(0).set(size, (double)RANDOMIZER.nextInt(MAX_HEIGHT + 1));
        height.get(size).set(0, (double)RANDOMIZER.nextInt(MAX_HEIGHT + 1));
        height.get(size).set(size, (double)RANDOMIZER.nextInt(MAX_HEIGHT + 1));

        // Computation of heights
        int  vertexNumber = 1, currentRange = size / 2, startPosition = size / 2;
        lastVertex.clear();
        lastVertex.add(new Vertex(startPosition, startPosition));
        while (currentRange > 0) {
            square(vertexNumber, currentRange); // square step
            diamond(vertexNumber, currentRange); // diamond step
            currentRange /= 2;
            vertexNumber *= 4;
        }

        // Search for minimum and maximum heights
        minValue = MAX_HEIGHT;
        maxValue = -MAX_HEIGHT;
        double currentMaxValue, currentMinValue;
        for (int y = 0; y <= size; y++) {
            currentMaxValue = Collections.max(height.get(y));
            currentMinValue = Collections.min(height.get(y));
            if (currentMaxValue > maxValue) {
                maxValue = currentMaxValue;
            }
            if (currentMinValue < minValue) {
                minValue = currentMinValue;
            }
        }
    }

    protected static void setColor(GL2 gl2, int x, int y) {
        double currentHeight = (height.get(y).get(x) - minValue) / (maxValue - minValue);
        currentHeight *= colorFactor;
        int roundCurrentHeight = (int) currentHeight;
        currentHeight = (double)roundCurrentHeight / colorFactor;

        if (color) {
            double redComponent = currentHeight * HILL_RED + (1 - currentHeight) * PLAIN_RED;
            double greenComponent = currentHeight * HILL_GREEN + (1 - currentHeight) * PLAIN_GREEN;
            double blueComponent = currentHeight * HILL_BLUE + (1 - currentHeight) * PLAIN_BLUE;
            gl2.glColor3d(redComponent, greenComponent, blueComponent);
        }
        else {
            gl2.glColor3d(currentHeight, currentHeight, currentHeight);
        }
    }

    protected static void setup(GL2 gl2, int w) {
        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glLoadIdentity();
        double[] first = {COS_F, SIN_F * SIN_T, SIN_F * COS_T, 0,
                0, COS_T, -SIN_T, 0,
                SIN_F, -COS_F * SIN_T, -COS_F * COS_T, 0,
                0, 0, 0, 1};

        gl2.glMultMatrixd(first, 0);
        gl2.glOrtho(-(float)SIZE_FRAME / 2, SIZE_FRAME, -(float)SIZE_FRAME / 2, SIZE_FRAME, -SIZE_FRAME * 2, SIZE_FRAME * 2);

        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glLoadIdentity();
        gl2.glEnable(GL2.GL_DEPTH_TEST);
        gl2.glViewport(0, 0, w, w);
        
        gl2.glClearColor(BACKGROUND_RED, BACKGROUND_GREEN, BACKGROUND_BLUE, 1f);
    }

    protected static void render(GL2 gl2) {
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl2.glLoadIdentity();

        if (bePressed) switch (keyCode) {
            case KeyEvent.VK_C:         // Change PolygonMode
                change = !change;
                break;
            case KeyEvent.VK_A:         // Move Left
                x -= MOVE_SPEED;
                break;
            case KeyEvent.VK_D:         // Move Right
                x += MOVE_SPEED;
                break;
            case KeyEvent.VK_W:         // Move up
                y += MOVE_SPEED;
                break;
            case KeyEvent.VK_S:         // Move Down
                y -= MOVE_SPEED;
                break;
            case KeyEvent.VK_Z:         // Zoom +
                zoom *= ZOOM_SPEED;
                break;
            case KeyEvent.VK_X:         // Zoom -
                zoom /= ZOOM_SPEED;
                break;
            case KeyEvent.VK_Q:         // Turn around OY Left
                if (aY == 0) angle = 0;
                aX = 0; aY = 1; aZ = 0;
                angle += ANGLE_SPEED;
                break;
            case KeyEvent.VK_E:         // Turn around OY Right
                if (aY == 0) angle = 0;
                aX = 0; aY = 1; aZ = 0;
                angle -= ANGLE_SPEED;
                break;
            case KeyEvent.VK_U:         // Turn around OX Straight
                if (aX == 0) angle = 0;
                aX = 1; aY = 0; aZ = 0;
                angle += ANGLE_SPEED;
                break;
            case KeyEvent.VK_J:         // Turn around OX Back
                if (aX == 0) angle = 0;
                aX = 1; aY = 0; aZ = 0;
                angle -= ANGLE_SPEED;
                break;
            case KeyEvent.VK_H:         // Turn around OZ Left
                if (aZ == 0) angle = 0;
                aX = 0; aY = 0; aZ = 1;
                angle += ANGLE_SPEED;
                break;
            case KeyEvent.VK_K:         // Turn around OZ Right
                if (aZ == 0) angle = 0;
                aX = 0; aY = 0; aZ = 1;
                angle -= ANGLE_SPEED;
                break;
            case KeyEvent.VK_V:         // Reduce the number of splits
                if (degree > 0) {
                    degree -= 1;
                    reFill = true;
                }
                break;
            case KeyEvent.VK_B:         // Increase the number of splits
                degree += 1;
                reFill = true;
                break;
            case KeyEvent.VK_R:         // Increase landscape sharpness
                landSharp += 0.1;
                reFill = true;
                break;
            case KeyEvent.VK_F:         // Reduce landscape sharpness
                if (landSharp > 0) {
                    landSharp -= 0.1;
                    reFill = true;
                }
                break;
            case KeyEvent.VK_T:         // Increase color smoothing factor
               colorFactor += 5;
                break;
            case KeyEvent.VK_G:         // Reduce color smoothing factor
                if (colorFactor > 5) {
                    colorFactor -= 5;
                }
                break;
            case KeyEvent.VK_N:         // Activate foundation mode
                foundation = !foundation;
                break;
            case KeyEvent.VK_M:         // Activate color mode
                color = !color;
                break;
        }

        //Fill vertex array
        if (reFill) fillHeightArray();
        reFill = false;
        bePressed = false;

        float ANGLE_COS = (float) Math.cos(Math.toRadians(angle)), ANGLE_SIN = (float) Math.sin(Math.toRadians(angle));
        float[] coordinates = {1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                x, y, 0, 1},
                ZOOM_MATRIX = {zoom, 0, 0, 0,
                        0, zoom, 0, 0,
                        0, 0, zoom, 0,
                        0, 0, 0, 1},
                TURN_MATRIX = {ANGLE_COS + (1 - ANGLE_COS) * aX * aX, ANGLE_SIN * aZ, -ANGLE_SIN * aY, 0,
                        -ANGLE_SIN * aZ, ANGLE_COS + (1 - ANGLE_COS) * aY * aY, ANGLE_SIN * aX, 0,
                        ANGLE_SIN * aY, -ANGLE_SIN * aX, ANGLE_COS + (1 - ANGLE_COS) * aZ * aZ, 0,
                        0, 0, 0, 1};

        gl2.glMultMatrixf(coordinates, 0);
        gl2.glMultMatrixf(TURN_MATRIX, 0);
        gl2.glMultMatrixf(ZOOM_MATRIX, 0);

        // Change PolygonMode
        if (change) {
            gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
        }
        else {
            gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        }

        // Drawing landscape
        gl2.glBegin(GL2.GL_QUADS);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                setColor(gl2, x, y);
                gl2.glVertex3d((double)(x * SIZE_LANDSCAPE) / size, height.get(y).get(x), (double)(y * SIZE_LANDSCAPE) / size);
                setColor(gl2, x + 1, y);
                gl2.glVertex3d((double)((x + 1) * SIZE_LANDSCAPE) / size, height.get(y).get(x + 1), (double)(y * SIZE_LANDSCAPE) / size);
                setColor(gl2, x + 1, y + 1);
                gl2.glVertex3d((double)((x + 1) * SIZE_LANDSCAPE) / size, height.get(y + 1).get(x + 1), (double)((y + 1) * SIZE_LANDSCAPE) / size);
                setColor(gl2, x, y + 1);
                gl2.glVertex3d((double)(x * SIZE_LANDSCAPE) / size, height.get(y + 1).get(x), (double)((y + 1) * SIZE_LANDSCAPE) / size);
            }
        }

        // Drawing borders and bottom
        if (!change && foundation) {
            gl2.glColor3f(BACKGROUND_RED, BACKGROUND_GREEN, BACKGROUND_BLUE);

            // Borders
            for (int y = 0; y <= size; y++) {
                if (y == 0 || y == size) {
                    for (int x = 0; x < size; x++) {
                        gl2.glVertex3d((double) (x * SIZE_LANDSCAPE) / size, height.get(y).get(x), (double) (y * SIZE_LANDSCAPE) / size);
                        gl2.glVertex3d((double) ((x + 1) * SIZE_LANDSCAPE) / size, height.get(y).get(x + 1), (double) (y * SIZE_LANDSCAPE) / size);
                        gl2.glVertex3d((double) ((x + 1) * SIZE_LANDSCAPE) / size, -MAX_HEIGHT, (double) (y * SIZE_LANDSCAPE) / size);
                        gl2.glVertex3d((double) (x * SIZE_LANDSCAPE) / size, -MAX_HEIGHT, (double) (y * SIZE_LANDSCAPE) / size);
                    }
                }
                if (y != size) {
                    gl2.glVertex3d(0, height.get(y).get(0), (double) (y * SIZE_LANDSCAPE) / size);
                    gl2.glVertex3d(0, height.get(y + 1).get(0), (double) ((y + 1) * SIZE_LANDSCAPE) / size);
                    gl2.glVertex3d(0, -MAX_HEIGHT, (double) ((y + 1) * SIZE_LANDSCAPE) / size);
                    gl2.glVertex3d(0, -MAX_HEIGHT, (double) (y * SIZE_LANDSCAPE) / size);

                    gl2.glVertex3d(SIZE_LANDSCAPE, height.get(y).get(size), (double) (y * SIZE_LANDSCAPE) / size);
                    gl2.glVertex3d(SIZE_LANDSCAPE, height.get(y + 1).get(size), (double) ((y + 1) * SIZE_LANDSCAPE) / size);
                    gl2.glVertex3d(SIZE_LANDSCAPE, -MAX_HEIGHT, (double) ((y + 1) * SIZE_LANDSCAPE) / size);
                    gl2.glVertex3d(SIZE_LANDSCAPE, -MAX_HEIGHT, (double) (y * SIZE_LANDSCAPE) / size);
                }
            }

            // Bottom
            gl2.glVertex3d(0, -MAX_HEIGHT, SIZE_LANDSCAPE);
            gl2.glVertex3d(0, -MAX_HEIGHT, 0);
            gl2.glVertex3d(SIZE_LANDSCAPE, -MAX_HEIGHT, 0);
            gl2.glVertex3d(SIZE_LANDSCAPE, -MAX_HEIGHT, SIZE_LANDSCAPE);
        }

        gl2.glEnd();
    }

    public static void main(String [] args) {
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities(glprofile);
        final GLCanvas glcanvas = new GLCanvas(glcapabilities);
        final FPSAnimator animator = new FPSAnimator(glcanvas, 60, true);
        animator.start();

        glcanvas.addGLEventListener(new GLEventListener() {

            @Override
            public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
                FractalLandscape.setup(glautodrawable.getGL().getGL2(), width);
            }

            @Override
            public void init(GLAutoDrawable glautodrawable) {
            }

            @Override
            public void dispose(GLAutoDrawable glautodrawable) {
            }

            @Override
            public void display(GLAutoDrawable glautodrawable) {
                FractalLandscape.render(glautodrawable.getGL().getGL2());
            }
        });

        final Frame frame = new Frame("FractalLandscape");
        frame.add(glcanvas);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowevent) {
                frame.remove(glcanvas);
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setSize(1250, 750);
        frame.setVisible(true);

        // KeyListeners
        glcanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keyCode = e.getKeyCode();
                bePressed = true;
            }
        });

    }
}