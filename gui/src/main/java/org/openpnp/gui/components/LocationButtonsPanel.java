/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.gui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;

/**
 * A JPanel of 4 small buttons that assist in setting locations. The buttons
 * are Capture Camera Coordinates, Capture Tool Coordinates, Move Camera
 * to Coordinates and Move Tool to Coordinates. If the actuatorId property
 * is set, this causes the component to use the specified Actuator in place
 * of the tool. 
 */
@SuppressWarnings("serial")
public class LocationButtonsPanel extends JPanel {
	private JTextField textFieldX, textFieldY, textFieldZ, textFieldC;
	private String actuatorId;

	private JButton buttonCenterTool;
	private JButton buttonCaptureCamera;
	private JButton buttonCaptureTool;

	public LocationButtonsPanel(JTextField textFieldX, JTextField textFieldY,
			JTextField textFieldZ, JTextField textFieldC) {
		FlowLayout flowLayout = (FlowLayout) getLayout();
		flowLayout.setVgap(0);
		flowLayout.setHgap(2);
		this.textFieldX = textFieldX;
		this.textFieldY = textFieldY;
		this.textFieldZ = textFieldZ;
		this.textFieldC = textFieldC;

		buttonCaptureCamera = new JButton(captureCameraCoordinatesAction);
		buttonCaptureCamera.setHideActionText(true);
		add(buttonCaptureCamera);

		buttonCaptureTool = new JButton(captureToolCoordinatesAction);
		buttonCaptureTool.setHideActionText(true);
		add(buttonCaptureTool);

		JButton buttonCenterCamera = new JButton(positionCameraAction);
		buttonCenterCamera.setHideActionText(true);
		add(buttonCenterCamera);

		buttonCenterTool = new JButton(positionToolAction);
		buttonCenterTool.setHideActionText(true);
		add(buttonCenterTool);

		setActuatorId(null);
	}

	public void setActuatorId(String actuatorId) {
		this.actuatorId = actuatorId;
		if (actuatorId == null || actuatorId.trim().length() == 0) {
			buttonCaptureTool.setAction(captureToolCoordinatesAction);
			buttonCenterTool.setAction(positionToolAction);
		}
		else {
			buttonCaptureTool.setAction(captureActuatorCoordinatesAction);
			buttonCenterTool.setAction(positionActuatorAction);
		}
	}

	public String getActuatorId() {
		return actuatorId;
	}

	private Location getParsedLocation() {
		Location location = new Location(Configuration.get().getSystemUnits());
		if (textFieldX != null) {
			location.setX(Length.parse(textFieldX.getText()).getValue());
		}
		if (textFieldY != null) {
			location.setY(Length.parse(textFieldY.getText()).getValue());
		}
		if (textFieldZ != null) {
			location.setZ(Length.parse(textFieldZ.getText()).getValue());
		}
		if (textFieldC != null) {
			location.setRotation(Double.parseDouble(textFieldC.getText()));
		}
		return location;
	}

	private Action captureCameraCoordinatesAction = new AbstractAction(
			"Get Camera Coordinates", new ImageIcon(
					LocationButtonsPanel.class
							.getResource("/icons/capture-camera.png"))) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Capture the location that the camera is centered on.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			Location l = MainFrame.machineControlsPanel.getCameraLocation();
			Helpers.copyLocationIntoTextFields(l, textFieldX, textFieldY,
					textFieldZ, textFieldC);
		}
	};

	private Action captureToolCoordinatesAction = new AbstractAction(
			"Get Tool Coordinates", new ImageIcon(
					LocationButtonsPanel.class
							.getResource("/icons/capture-tool.png"))) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Capture the location that the tool is centered on.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			Location l = MainFrame.machineControlsPanel.getToolLocation();
			Helpers.copyLocationIntoTextFields(l, textFieldX, textFieldY,
					textFieldZ, textFieldC);
		}
	};

	private Action captureActuatorCoordinatesAction = new AbstractAction(
			"Get Actuator Coordinates", new ImageIcon(
					LocationButtonsPanel.class
							.getResource("/icons/capture-pin.png"))) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Capture the location that the actuator is centered on.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if (actuatorId == null) {
				return;
			}
			
			Head head = Configuration.get().getMachine().getHeads()
					.get(0);
			Actuator actuator = head.getActuator(actuatorId);
			if (actuator == null) {
				MessageBoxes.errorBox(getTopLevelAncestor(),
						"Error", String.format(
								"No Actuator with ID %s on Head %s",
								actuatorId, head.getId()));
			}
			
			Location location = MainFrame.machineControlsPanel.getToolLocation();
			location = location.add(actuator.getLocation());
			
			Helpers.copyLocationIntoTextFields(location, textFieldX, textFieldY,
					textFieldZ, textFieldC);
		}
	};

	private Action positionCameraAction = new AbstractAction("Position Camera",
			new ImageIcon(
					LocationButtonsPanel.class
							.getResource("/icons/center-camera.png"))) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Position the camera over the center of the location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = Configuration.get().getMachine().getHeads()
							.get(0);
					Camera camera = MainFrame.cameraPanel.getSelectedCamera();
					Location location = getParsedLocation().convertToUnits(
							head.getMachine().getNativeUnits());
					location = location.subtract(camera.getLocation());
					try {
						head.moveToSafeZ();
						// Move the head to the right position at Safe-Z
						head.moveTo(location.getX(), location.getY(),
								head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Movement Error", e);
					}
				}
			});
		}
	};

	private Action positionToolAction = new AbstractAction("Position Tool",
			new ImageIcon(
					LocationButtonsPanel.class
							.getResource("/icons/center-tool.png"))) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Position the tool over the center of the location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = Configuration.get().getMachine().getHeads()
							.get(0);
					Location location = getParsedLocation().convertToUnits(
							head.getMachine().getNativeUnits());
					try {
						head.moveToSafeZ();
						// Move the head to the right position at Safe-Z
						head.moveTo(location.getX(), location.getY(),
								head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Movement Error", e);
					}
				}
			});
		}
	};

	private Action positionActuatorAction = new AbstractAction(
			"Position Actuator", new ImageIcon(
					LocationButtonsPanel.class
							.getResource("/icons/center-pin.png"))) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Position the actuator over the center of the location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (actuatorId == null) {
				return;
			}
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = Configuration.get().getMachine().getHeads()
							.get(0);
					Actuator actuator = head.getActuator(actuatorId);
					if (actuator == null) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Positioning Error", String.format(
										"No Actuator with ID %s on Head %s",
										actuatorId, head.getId()));
					}
					Location location = getParsedLocation().convertToUnits(
							head.getMachine().getNativeUnits());
					location = location.subtract(actuator.getLocation());
					try {
						head.moveToSafeZ();
						// Move the head to the right position at Safe-Z
						head.moveTo(location.getX(), location.getY(),
								head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Movement Error", e);
					}
				}
			});
		}
	};
}
