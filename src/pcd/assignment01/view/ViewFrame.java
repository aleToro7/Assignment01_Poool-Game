package pcd.assignment01.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import pcd.assignment01.util.RenderSynch;

public class ViewFrame extends JFrame {

	private VisualiserPanel panel;
	private ViewModel model;
	private RenderSynch sync;

	public ViewFrame(ViewModel model, int w, int h){
		this.model = model;
		this.sync = new RenderSynch();
		setTitle("Poool Sketch");
		setSize(w,h + 25);
		setResizable(false);
		panel = new VisualiserPanel(w,h);
		getContentPane().add(panel);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent ev){
				System.exit(-1);
			}
			public void windowClosed(WindowEvent ev){
				System.exit(-1);
			}
		});
	}

	public void render(){
		long nf = sync.nextFrameToRender();
		panel.repaint();
		try {
			sync.waitForFrameRendered(nf);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	public class VisualiserPanel extends JPanel {
		private int ox;
		private int oy;
		private int delta;

		public VisualiserPanel(int w, int h){
			setSize(w,h + 25);
			ox = w/2;
			oy = h/2;
			delta = Math.min(ox, oy);
		}

		public void paint(Graphics g){
			Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.clearRect(0,0,this.getWidth(),this.getHeight());

			// 1. Disegno le linee del campo (griglia a croce)
			g2.setColor(Color.LIGHT_GRAY);
			g2.setStroke(new BasicStroke(1));
			g2.drawLine(ox,0,ox,oy*2);
			g2.drawLine(0,oy,ox*2,oy);

			// 2. Disegno le Buche (angoli in alto a sinistra e in alto a destra)
			g2.setColor(Color.BLACK);
			int holeRadius = 100; // Grandezza della buca (modificabile)
			// Buca sinistra (quarto di cerchio nell'angolo 0,0)
			g2.fillArc(-holeRadius, -holeRadius, holeRadius*2, holeRadius*2, 270, 90);
			// Buca destra (quarto di cerchio nell'angolo larghezza, 0)
			g2.fillArc(getWidth() - holeRadius, -holeRadius, holeRadius*2, holeRadius*2, 180, 90);

			// 3. Disegno i Punteggi in blu
			g2.setColor(Color.BLUE);
			g2.setFont(new Font("Arial", Font.PLAIN, 120)); // Font grande per i numeri
			FontMetrics fm = g2.getFontMetrics();

			String score1Str = String.valueOf(model.getScore1());
			String score2Str = String.valueOf(model.getScore2());

			// Posiziono i punteggi centrati nei due quadranti inferiori
			int score1X = (ox / 2) - (fm.stringWidth(score1Str) / 2);
			int score2X = (ox + (ox / 2)) - (fm.stringWidth(score2Str) / 2);
			int scoreY = oy + (oy / 2) + (fm.getAscent() / 2) - 20; // Abbassato leggermente

			g2.drawString(score1Str, score1X, scoreY);
			g2.drawString(score2Str, score2X, scoreY);

			// 4. Disegno lo sciame di palline piccole
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1));
			for (var b: model.getBalls()) {
				var p = b.pos();
				int x0 = (int)(ox + p.x()*delta);
				int y0 = (int)(oy - p.y()*delta);
				int radiusX = (int)(b.radius()*delta);
				int radiusY = (int)(b.radius()*delta);
				g2.drawOval(x0 - radiusX, y0 - radiusY, radiusX*2, radiusY*2);
			}

			// 5. Disegno le Palline dei Giocatori (più spesse)
			g2.setStroke(new BasicStroke(3));
			g2.setFont(new Font("Arial", Font.BOLD, 16)); // Font per le lettere H e B
			FontMetrics fmLetter = g2.getFontMetrics();

			// Disegno Giocatore 1 (H)
			var p1 = model.getPlayer1();
			if (p1 != null) {
				drawPlayerBall(g2, p1, "H", fmLetter);
			}

			// Disegno Giocatore 2 (B)
			var p2 = model.getPlayer2();
			if (p2 != null) {
				drawPlayerBall(g2, p2, "B", fmLetter);
			}

			sync.notifyFrameRendered();
		}

		// Metodo di supporto per disegnare le palline dei giocatori per evitare ripetizioni
		private void drawPlayerBall(Graphics2D g2, BallViewInfo pb, String label, FontMetrics fm) {
			var p = pb.pos();
			int x0 = (int)(ox + p.x()*delta);
			int y0 = (int)(oy - p.y()*delta);
			int radiusX = (int)(pb.radius()*delta);
			int radiusY = (int)(pb.radius()*delta);

			g2.drawOval(x0 - radiusX, y0 - radiusY, radiusX*2, radiusY*2);

			// Centro la lettera dentro la pallina
			int textX = x0 - (fm.stringWidth(label) / 2);
			int textY = y0 + (fm.getAscent() / 2) - 2; // -2 per centrare verticalmente al meglio
			g2.drawString(label, textX, textY);
		}
	}
}