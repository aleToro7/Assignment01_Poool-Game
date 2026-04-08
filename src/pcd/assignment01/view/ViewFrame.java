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
			public void windowClosing(WindowEvent ev){ System.exit(-1); }
			public void windowClosed(WindowEvent ev){ System.exit(-1); }
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

			g2.setColor(Color.LIGHT_GRAY);
			g2.setStroke(new BasicStroke(1));
			g2.drawLine(ox,0,ox,oy*2);
			g2.drawLine(0,oy,ox*2,oy);

			g2.setColor(Color.BLACK);
			int holeRadius = 100;
			g2.fillArc(-holeRadius, -holeRadius, holeRadius*2, holeRadius*2, 270, 90);
			g2.fillArc(getWidth() - holeRadius, -holeRadius, holeRadius*2, holeRadius*2, 180, 90);

			g2.setColor(Color.BLUE);
			g2.setFont(new Font("Arial", Font.PLAIN, 120));
			FontMetrics fmScore = g2.getFontMetrics();

			String score1Str = String.valueOf(model.getScore1());
			String score2Str = String.valueOf(model.getScore2());

			int score1X = (ox / 2) - (fmScore.stringWidth(score1Str) / 2);
			int score2X = (ox + (ox / 2)) - (fmScore.stringWidth(score2Str) / 2);
			int scoreY = oy + (oy / 2) + (fmScore.getAscent() / 2) - 20;

			g2.drawString(score1Str, score1X, scoreY);
			g2.drawString(score2Str, score2X, scoreY);

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

			g2.setStroke(new BasicStroke(3));
			g2.setFont(new Font("Arial", Font.BOLD, 16));
			FontMetrics fmLetter = g2.getFontMetrics();

			var p1 = model.getPlayer1();
			if (p1 != null) drawPlayerBall(g2, p1, "H", fmLetter);

			var p2 = model.getPlayer2();
			if (p2 != null) drawPlayerBall(g2, p2, "B", fmLetter);

			// Messaggio di Game Over
			if (model.isGameOver()) {
				g2.setColor(Color.RED);
				g2.setFont(new Font("Arial", Font.BOLD, 36));
				FontMetrics fmGO = g2.getFontMetrics();
				String msg = model.getWinnerMessage();

				// Centriamo il testo orizzontalmente e verticalmente
				int msgX = (this.getWidth() - fmGO.stringWidth(msg)) / 2;
				int msgY = (this.getHeight() + fmGO.getAscent()) / 2 - 50;

				// Disegniamo uno sfondo semitrasparente bianco dietro la scritta per farla leggere meglio
				g2.setColor(new Color(255, 255, 255, 200));
				g2.fillRect(msgX - 20, msgY - fmGO.getAscent() - 10, fmGO.stringWidth(msg) + 40, fmGO.getHeight() + 20);

				g2.setColor(Color.RED);
				g2.drawString(msg, msgX, msgY);
			}

			sync.notifyFrameRendered();
		}

		private void drawPlayerBall(Graphics2D g2, BallViewInfo pb, String label, FontMetrics fm) {
			var p = pb.pos();
			int x0 = (int)(ox + p.x()*delta);
			int y0 = (int)(oy - p.y()*delta);
			int radiusX = (int)(pb.radius()*delta);
			int radiusY = (int)(pb.radius()*delta);

			g2.drawOval(x0 - radiusX, y0 - radiusY, radiusX*2, radiusY*2);

			int textX = x0 - (fm.stringWidth(label) / 2);
			int textY = y0 + (fm.getAscent() / 2) - 2;
			g2.drawString(label, textX, textY);
		}
	}
}