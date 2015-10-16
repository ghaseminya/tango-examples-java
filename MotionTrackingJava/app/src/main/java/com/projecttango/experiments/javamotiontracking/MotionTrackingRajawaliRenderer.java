/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.experiments.javamotiontracking;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.TouchViewHandler;
import com.projecttango.rajawali.renderables.FrustumAxes;
import com.projecttango.rajawali.renderables.Grid;
import com.projecttango.rajawali.ScenePoseCalcuator;
import com.projecttango.rajawali.renderables.Trajectory;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.RajawaliRenderer;

/**
 * This class implements the rendering logic for the Motion Tracking application using Rajawali.
 */
public class MotionTrackingRajawaliRenderer extends RajawaliRenderer {

    private final String TAG = ScenePoseCalcuator.class.getSimpleName();

    // Only add line segments to the trajectory if the deviced moved more than THRESHOLD meters
    private static final double THRESHOLD = 0.002f;

    private static final float CAMERA_NEAR = 0.01f;
    private static final float CAMERA_FAR = 200f;

    private FrustumAxes mFrustumAxes;
    private FrustumAxes mOther;
    private Trajectory mTrajectory;

    private TouchViewHandler touchViewHandler;

    // Latest available device pose;
    private Pose mDevicePose = new Pose(Vector3.ZERO, Quaternion.getIdentity());
    private boolean mPoseUpdated = false;

    public MotionTrackingRajawaliRenderer(Context context) {
        super(context);
        touchViewHandler = new TouchViewHandler(mContext, getCurrentCamera());
    }

    @Override
    protected void initScene() {
        Grid grid = new Grid(100, 1, 1, 0xFFCCCCCC);
        grid.setPosition(0, -1.3f, 0);
        getCurrentScene().addChild(grid);

        mFrustumAxes = new FrustumAxes(3);

        mOther = new FrustumAxes(1);
        mOther.setPosition(0, 0, 0);
        getCurrentScene().addChild(mFrustumAxes);
        getCurrentScene().addChild(mOther);

        mTrajectory = new Trajectory(Color.BLUE, 2);
        getCurrentScene().addChild(mTrajectory);

        getCurrentScene().setBackgroundColor(Color.WHITE);

        getCurrentCamera().setNearPlane(CAMERA_NEAR);
        getCurrentCamera().setFarPlane(CAMERA_FAR);
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);

        // Update the scene objects with the latest device position and orientation information.
        // Synchronize to avoid concurrent access from the Tango callback thread below.
        synchronized (this) {
            if (mPoseUpdated) {
                mPoseUpdated = false;
                mFrustumAxes.setPosition(mDevicePose.getPosition());
                mFrustumAxes.setOrientation(mDevicePose.getOrientation());

                if (mTrajectory.getLastPoint().distanceTo2(mDevicePose.getPosition()) > THRESHOLD) {
                    mTrajectory.addSegmentTo(mDevicePose.getPosition());
                }

                touchViewHandler.updateCamera(mDevicePose.getPosition(), mDevicePose.getOrientation());
            }
        }
    }

    /**
     * Updates our information about the current device pose.
     * This is called from the Tango service thread through the callback API. Synchronize to avoid
     * concurrent access from the OpenGL thread above.
     */
    public synchronized void updateDevicePose(TangoPoseData tangoPoseData) {
        mDevicePose = ScenePoseCalcuator.toOpenGLPose(tangoPoseData);
        mPoseUpdated = true;
    }

    public void updateOtherPosition(float[] translation) {
        mOther.setPosition(translation[0], translation[2], -translation[1]);
    }

    public void updateOtherPose(float[] translation, float[] orientation) {
        TangoPoseData pose = new TangoPoseData();
        pose.translation[0] = translation[0];
        pose.translation[1] = translation[1];
        pose.translation[2] = translation[2];

        pose.rotation[0] = orientation[0];
        pose.rotation[1] = orientation[1];
        pose.rotation[2] = orientation[2];
        pose.rotation[3] = orientation[3];

        Pose otherPose = ScenePoseCalcuator.toOpenGLPose(pose);
        mOther.setPosition(otherPose.getPosition());
        mOther.setOrientation(otherPose.getOrientation());
    }

    @Override
    public void onOffsetsChanged(float v, float v1, float v2, float v3, int i, int i1) {

    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {
        touchViewHandler.onTouchEvent(motionEvent);
    }

    public void setFirstPersonView() {
        touchViewHandler.setFirstPersonView();
    }

    public void setTopDownView() {
        touchViewHandler.setTopDownView();
    }

    public void setThirdPersonView() {
        touchViewHandler.setThirdPersonView();
    }
}