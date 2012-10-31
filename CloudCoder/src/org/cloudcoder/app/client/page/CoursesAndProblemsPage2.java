// CloudCoder - a web-based pedagogical programming environment
// Copyright (C) 2011-2012, Jaime Spacco <jspacco@knox.edu>
// Copyright (C) 2011-2012, David H. Hovemeyer <david.hovemeyer@gmail.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.cloudcoder.app.client.page;

import org.cloudcoder.app.client.model.Session;
import org.cloudcoder.app.client.model.StatusMessage;
import org.cloudcoder.app.client.rpc.RPC;
import org.cloudcoder.app.client.view.PageNavPanel;
import org.cloudcoder.app.client.view.ProblemDescriptionView;
import org.cloudcoder.app.client.view.ProblemListView2;
import org.cloudcoder.app.client.view.StatusMessageView;
import org.cloudcoder.app.client.view.TermAndCourseTreeView;
import org.cloudcoder.app.shared.model.CloudCoderAuthenticationException;
import org.cloudcoder.app.shared.model.Course;
import org.cloudcoder.app.shared.model.CourseAndCourseRegistration;
import org.cloudcoder.app.shared.model.CourseRegistrationType;
import org.cloudcoder.app.shared.model.Problem;
import org.cloudcoder.app.shared.model.ProblemAndSubmissionReceipt;
import org.cloudcoder.app.shared.util.Publisher;
import org.cloudcoder.app.shared.util.Subscriber;
import org.cloudcoder.app.shared.util.SubscriptionRegistrar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.view.client.SelectionChangeEvent;

/**
 * A {@link CloudCoderPage} to browse courses and problems.
 * 
 * @author David Hovemeyer
 */
public class CoursesAndProblemsPage2 extends CloudCoderPage {
    /**
     * UI class for CoursesAndProblemsPage2.
     */
    private class UI extends Composite implements SessionObserver, Subscriber {
        /**
		 * Width of the course and user admin buttons.
		 */
		public static final double COURSE_AND_USER_ADMIN_BUTTON_WIDTH_PX = 120.0;

		/**
		 * Height of the course and user admin buttons.
		 */
		private static final double COURSE_AND_USER_ADMIN_BUTTON_HEIGHT_PX = 27.0;

        private LayoutPanel eastLayoutPanel;

        private PageNavPanel pageNavPanel;
        private TermAndCourseTreeView termAndCourseTreeView;
        private ProblemDescriptionView problemDescriptionView;
        private StatusMessageView statusMessageView;
        private ProblemListView2 problemListView2;
        private Button courseAdminButton;
        private Button userAdminButton;
        private Button loadProblemButton;
        private Button accountButton;

        public UI() {
            DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Unit.PX);

            this.eastLayoutPanel = new LayoutPanel();

            this.pageNavPanel = new PageNavPanel();
            pageNavPanel.setShowBackButton(false);
            eastLayoutPanel.add(pageNavPanel);
            eastLayoutPanel.setWidgetTopHeight(pageNavPanel, 0.0, Unit.PX, PageNavPanel.HEIGHT, PageNavPanel.HEIGHT_UNIT);
            eastLayoutPanel.setWidgetLeftRight(pageNavPanel, 0.0, Unit.PX, 0.0, Unit.PX);

            dockLayoutPanel.addEast(eastLayoutPanel, 225.0);

            LayoutPanel southLayoutPanel = new LayoutPanel();

            this.statusMessageView = new StatusMessageView();
            southLayoutPanel.add(statusMessageView);
            southLayoutPanel.setWidgetLeftRight(statusMessageView, 0.0, Unit.PX, 0.0, Unit.PX);
            southLayoutPanel.setWidgetTopHeight(statusMessageView, 0.0, Unit.PX, StatusMessageView.HEIGHT_PX, Unit.PX);

            this.loadProblemButton = new Button("Load problem!");
            loadProblemButton.setStylePrimaryName("cc-emphButton");
            loadProblemButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    loadProblemButtonClicked();
                }
            });
            southLayoutPanel.add(loadProblemButton);
            southLayoutPanel.setWidgetRightWidth(loadProblemButton, 0.0, Unit.PX, 160.0, Unit.PX);
            southLayoutPanel.setWidgetTopHeight(loadProblemButton, StatusMessageView.HEIGHT_PX, Unit.PX, StatusMessageView.HEIGHT_PX, Unit.PX);

            this.problemDescriptionView = new ProblemDescriptionView();
            southLayoutPanel.add(problemDescriptionView);
            southLayoutPanel.setWidgetLeftRight(problemDescriptionView, 0.0, Unit.PX, 0.0, Unit.PX);
            southLayoutPanel.setWidgetTopBottom(problemDescriptionView, StatusMessageView.HEIGHT_PX*2, Unit.PX, 0.0, Unit.PX);

            dockLayoutPanel.addSouth(southLayoutPanel, ProblemDescriptionView.HEIGHT_PX + StatusMessageView.HEIGHT_PX*2);

            this.problemListView2 = new ProblemListView2(CoursesAndProblemsPage2.this);
            dockLayoutPanel.add(problemListView2);

            initWidget(dockLayoutPanel);
        }

        private void loadProblemButtonClicked() {
            Problem problem = getSession().get(Problem.class);
            if (problem != null) {
                getSession().notifySubscribers(Session.Event.PROBLEM_CHOSEN, problem);
            }
        }

        /* (non-Javadoc)
         * @see org.cloudcoder.app.client.page.SessionObserver#activate(org.cloudcoder.app.client.model.Session, org.cloudcoder.app.shared.util.SubscriptionRegistrar)
         */
        @Override
        public void activate(final Session session, final SubscriptionRegistrar subscriptionRegistrar) {
            session.subscribe(Session.Event.ADDED_OBJECT, this, subscriptionRegistrar);

            // activate views
            problemListView2.activate(session, subscriptionRegistrar);
            problemDescriptionView.activate(session, subscriptionRegistrar);
            statusMessageView.activate(session, subscriptionRegistrar);

            // register a logout handler
            this.pageNavPanel.setLogoutHandler(new LogoutHandler(session));

            // Load courses
            getCoursesAndProblemsRPC(session);
        }

		protected void getCoursesAndProblemsRPC(final Session session) {
			RPC.getCoursesAndProblemsService.getCourseAndCourseRegistrations(new AsyncCallback<CourseAndCourseRegistration[]>() {
                @Override
                public void onSuccess(CourseAndCourseRegistration[] result) {
                    GWT.log(result.length + " course(s) loaded");
                    addSessionObject(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                	if (caught instanceof CloudCoderAuthenticationException) {
                		recoverFromServerSessionTimeout(new Runnable() {
                			@Override
                			public void run() {
                				// Try again!
                				getCoursesAndProblemsRPC(session);
                			}
                		});
                	} else {
	                    GWT.log("Error loading courses", caught);
	                    session.add(StatusMessage.error("Error loading courses", caught));
                	}
                }
            });
		}

        /* (non-Javadoc)
         * @see org.cloudcoder.app.shared.util.Subscriber#eventOccurred(java.lang.Object, org.cloudcoder.app.shared.util.Publisher, java.lang.Object)
         */
        @Override
        public void eventOccurred(Object key, Publisher publisher, Object hint) {
            if (key == Session.Event.ADDED_OBJECT && hint instanceof CourseAndCourseRegistration[]) {
                CourseAndCourseRegistration[] courseAndRegList = (CourseAndCourseRegistration[]) hint;

                boolean isInstructor = false;

                // Determine if the user is an instructor for any of the courses
                for (CourseAndCourseRegistration courseAndReg : courseAndRegList) {
                    if (courseAndReg.getCourseRegistration().getRegistrationType() == CourseRegistrationType.INSTRUCTOR) {
                        GWT.log("Instructor for course " +  courseAndReg.getCourse().getName());
                        isInstructor = true;
                    }
                }

                // Courses are loaded - create and activate TermAndCourseTreeView.
                // If the user is an instructor for at least one course, leave some room for
                // the "Course admin" button.
                termAndCourseTreeView = new TermAndCourseTreeView(courseAndRegList);
                eastLayoutPanel.add(termAndCourseTreeView);
                eastLayoutPanel.setWidgetLeftRight(termAndCourseTreeView, 8.0, Unit.PX, 0.0, Unit.PX);
                eastLayoutPanel.setWidgetTopBottom(
                        termAndCourseTreeView,
                        PageNavPanel.HEIGHT + (isInstructor ? COURSE_AND_USER_ADMIN_BUTTON_HEIGHT_PX + 70.0 : 10),
                        PageNavPanel.HEIGHT_UNIT,
                        0.0,
                        Unit.PX);

                accountButton = new Button("My Account");
                accountButton.setEnabled(false);
                eastLayoutPanel.add(accountButton);
                eastLayoutPanel.setWidgetRightWidth(accountButton, 0.0, Unit.PX, COURSE_AND_USER_ADMIN_BUTTON_WIDTH_PX, Unit.PX);
                eastLayoutPanel.setWidgetTopHeight(accountButton, PageNavPanel.HEIGHT + 4.0, PageNavPanel.HEIGHT_UNIT, COURSE_AND_USER_ADMIN_BUTTON_HEIGHT_PX, Unit.PX);
                accountButton.addClickHandler(new ClickHandler(){
                    /* (non-Javadoc)
                     * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
                     */
                    @Override
                    public void onClick(ClickEvent event) {
                        handleAccountButtonClicked();
                    }
                });
                
                // Create the "Course admin" button if appropriate.
                if (isInstructor) {
                    courseAdminButton = new Button("Course admin");
                    eastLayoutPanel.add(courseAdminButton);
                    eastLayoutPanel.setWidgetRightWidth(courseAdminButton, 0.0, Unit.PX, COURSE_AND_USER_ADMIN_BUTTON_WIDTH_PX, Unit.PX);
                    eastLayoutPanel.setWidgetTopHeight(courseAdminButton, 2 * PageNavPanel.HEIGHT + 4.0, PageNavPanel.HEIGHT_UNIT, COURSE_AND_USER_ADMIN_BUTTON_HEIGHT_PX, Unit.PX);
                    courseAdminButton.addClickHandler(new ClickHandler(){
                        /* (non-Javadoc)
                         * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
                         */
                        @Override
                        public void onClick(ClickEvent event) {
                            handleCourseAdminButtonClicked();
                        }
                    });

                    userAdminButton = new Button("User admin");
                    eastLayoutPanel.add(userAdminButton);
                    eastLayoutPanel.setWidgetRightWidth(userAdminButton, 0.0, Unit.PX, COURSE_AND_USER_ADMIN_BUTTON_WIDTH_PX, Unit.PX);
                    eastLayoutPanel.setWidgetTopHeight(userAdminButton, 3 * PageNavPanel.HEIGHT + 4.0, PageNavPanel.HEIGHT_UNIT, COURSE_AND_USER_ADMIN_BUTTON_HEIGHT_PX, Unit.PX);
                    userAdminButton.addClickHandler(new ClickHandler(){
                        /* (non-Javadoc)
                         * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
                         */
                        @Override
                        public void onClick(ClickEvent event) {
                            handleUserAdminButtonClicked();
                        }
                    });

                    // Disable buttons initially.  They will be enabled/disabled
                    // appropriately as courses are selected.
                    courseAdminButton.setEnabled(false);
                    userAdminButton.setEnabled(false);
                }

                // add selection event handler
                termAndCourseTreeView.addSelectionHandler(new SelectionChangeEvent.Handler() {
                    @Override
                    public void onSelectionChange(SelectionChangeEvent event) {
                        Course course = termAndCourseTreeView.getSelectedCourse();
                        getSession().add(course);
                    }
                });
            } else if (key == Session.Event.ADDED_OBJECT && hint instanceof Course) {
                Course course = (Course) hint;

                // Load problems
                SessionUtil.loadProblemAndSubmissionReceiptsInCourse(CoursesAndProblemsPage2.this, course, getSession());

                if (courseAdminButton != null || userAdminButton != null) {
                    // Find the CourseRegistration for this Course
                    CourseAndCourseRegistration[] courseAndRegList = getSession().get(CourseAndCourseRegistration[].class);
                    for (CourseAndCourseRegistration courseAndReg : courseAndRegList) {
                        if (courseAndReg.getCourse() == course) {
                            // Enable or disable the courseAdminButton (and userAdminButton) depending on whether or not
                            // user is an instructor.
                            boolean isInstructor = courseAndReg.getCourseRegistration().getRegistrationType() == CourseRegistrationType.INSTRUCTOR;
                            courseAdminButton.setEnabled(isInstructor);
                            userAdminButton.setEnabled(isInstructor);
                            GWT.log((isInstructor ? "enable" : "disable") + " courseAdminButton");
                        }
                    }
                }
                
                accountButton.setEnabled(true);
            }
        }

        protected void handleCourseAdminButtonClicked() {
            GWT.log("Course admin button clicked");
            Course course = getSession().get(Course.class);
            if (course != null) {
                getSession().notifySubscribers(Session.Event.COURSE_ADMIN, course);
            }
        }

        protected void handleUserAdminButtonClicked() {
            GWT.log("User admin button clicked");
            Course course = getSession().get(Course.class);
            if (course != null) {
                getSession().notifySubscribers(Session.Event.USER_ADMIN, course);
            }
        }
        
        protected void handleAccountButtonClicked() {
            GWT.log("My account button clicked");
            Course course = getSession().get(Course.class);
            if(course != null) {
            	getSession().notifySubscribers(Session.Event.USER_ACCOUNT, course);
            }
        }
    }

    private UI ui;

    /* (non-Javadoc)
     * @see org.cloudcoder.app.client.page.CloudCoderPage#createWidget()
     */
    @Override
    public void createWidget() {
        ui = new UI();
    }

    /* (non-Javadoc)
     * @see org.cloudcoder.app.client.page.CloudCoderPage#activate()
     */
    @Override
    public void activate() {
        getSession().add(new ProblemAndSubmissionReceipt[0]);
        ui.activate(getSession(), getSubscriptionRegistrar());
    }

    /* (non-Javadoc)
     * @see org.cloudcoder.app.client.page.CloudCoderPage#deactivate()
     */
    @Override
    public void deactivate() {
        getSubscriptionRegistrar().cancelAllSubscriptions();
    }

    /* (non-Javadoc)
     * @see org.cloudcoder.app.client.page.CloudCoderPage#getWidget()
     */
    @Override
    public IsWidget getWidget() {
        return ui;
    }

    /* (non-Javadoc)
     * @see org.cloudcoder.app.client.page.CloudCoderPage#isActivity()
     */
    @Override
    public boolean isActivity() {
        return true;
    }

}
