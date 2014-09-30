/* 
 * SparkBit
 *
 * Copyright 2011-2014 multibit.org
 * Copyright 2014 Coin Sciences Ltd
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.multibit.controller.core;

import org.sparkbit.ApplicationDataDirectoryLocator;
import org.sparkbit.SparkBit;
import org.multibit.controller.AbstractEventHandler;
import org.multibit.controller.BaseController;
import org.multibit.model.core.CoreModel;
import org.multibit.platform.listener.*;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.ViewSystem;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.action.ExitAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 *
 * @author Cameron Garnham
 */
public class CoreController extends BaseController<CoreController> implements GenericOpenURIEventListener, GenericPreferencesEventListener,
        GenericAboutEventListener, GenericQuitEventListener {
    
    private Logger log = LoggerFactory.getLogger(CoreController.class);
    
    private Set<AbstractEventHandler> eventHandlers;
    private CoreController.EventHandler eventHandler;
    
    /**
     * The data model backing the views.
     */
    private CoreModel model;
    
    public CoreController() {
        this(null);
    }
    
    public CoreController(ApplicationDataDirectoryLocator applicationDataDirectoryLocator) {
        super(applicationDataDirectoryLocator);
        
        this.eventHandlers = new HashSet<AbstractEventHandler>();
        this.eventHandler = new EventHandler(this);
        
        this.addEventHandler(this.getEventHandler());
    }
    
    
    @Override
    public CoreModel getModel() {
        return model;
    }

    public void setModel(CoreModel model) {
        this.model = model;
    }
    
    
    
    /**
     * Display the view specified.
     * 
     * @param viewToDisplay
     *            View to display. Must be one of the View constants
     */
    @Override
    public void displayView(View viewToDisplay) {
        log.debug("Displaying view '" + viewToDisplay + "'");

        // Tell all views to close the current view.
        for (ViewSystem viewSystem : super.getViewSystem()) {
            viewSystem.navigateAwayFromView(getCurrentView());
        }

        setCurrentView(viewToDisplay);

        // Tell all views which view to display.
        for (ViewSystem viewSystem : super.getViewSystem()) {
            viewSystem.displayView(getCurrentView());
        }
    }
    
    /**
     * Display the help context specified.
     * 
     * @param helpContextToDisplay
     *            The help context to display. A path in the help
     */
    @Override
    public void displayHelpContext(String helpContextToDisplay) {
        //log.debug("Displaying help context '" + helpContextToDisplay + "'");
        
        // Tell all views to close the current view.
        for (ViewSystem viewSystem : super.getViewSystem()) {
            viewSystem.navigateAwayFromView(getCurrentView());
        }

        setCurrentView(View.HELP_CONTENTS_VIEW);

        // Tell all views which view to display.
        for (ViewSystem viewSystem : super.getViewSystem()) {
            viewSystem.setHelpContext(helpContextToDisplay);
            viewSystem.displayView(View.HELP_CONTENTS_VIEW);
        }
    }
    
    /**
     * The language has been changed.
     */
    @Override
    public void fireDataStructureChanged() {
        //log.debug("fireDataStructureChanged called");

        Locale newLocale = new Locale(model.getUserPreference(CoreModel.USER_LANGUAGE_CODE));
        super.getLocaliser().setLocale(newLocale);

        View viewToDisplay = getCurrentView();

        // tell the viewSystems to refresh their views
        for (ViewSystem viewSystem : super.getViewSystem()) {
            viewSystem.recreateAllViews(true, viewToDisplay);
        }

        setCurrentView(viewToDisplay);
        fireDataChangedUpdateNow();
    }
    
    @Override
    public View getCurrentView() {
        View view = (null == getModel()) ? null : getModel().getCurrentView();
        
        return (null == view) ? View.DEFAULT_VIEW() : view;
    }

    @Override
    public void setCurrentView(View view) {
        // log.debug("setCurrentView = " + view);
        if (getModel() != null) {
            getModel().setCurrentView(view);
        }
    }
    
    @Override
    public void onPreferencesEvent(GenericPreferencesEvent event) {
        displayView(View.PREFERENCES_VIEW);
    }

    @Override
    public void onAboutEvent(GenericAboutEvent event) {
        displayView(View.HELP_ABOUT_VIEW);
    }
    
    @Override
    public synchronized void onOpenURIEvent(GenericOpenURIEvent event) {
        log.debug("Controller received open URI event with URI='{}'", event.getURI().toASCIIString());
        if (!getApplicationStarting()) {
            log.debug("Open URI event handled immediately");

            for (AbstractEventHandler theEventHandler : this.eventHandlers) {
                theEventHandler.handleOpenURIEvent(event.getURI());
            }
               
        } else {
            log.debug("Open URI event not handled immediately because application is still starting. Remembering for later.");
            SparkBit.setRememberedRawBitcoinURI(event.getURI().toASCIIString());
        }
    }
    
    @Override
    public void onQuitEvent(GenericQuitEvent event, GenericQuitResponse response) {
        if (isOKToQuit()) {
            
            ExitAction exitAction;
            if (super.getViewSystem() != null) {
                Iterator<ViewSystem> iterator = super.getViewSystem().iterator();
                ViewSystem viewSystemLoop = iterator.next();
                if (viewSystemLoop instanceof MultiBitFrame) {
                    exitAction = new ExitAction(this, (MultiBitFrame)viewSystemLoop);                    
                } else {
                    exitAction = new ExitAction(this, null);                    
                }
            } else {
                exitAction = new ExitAction(this, null);
            }
            
            for (AbstractEventHandler theEventHandler : this.eventHandlers) {
                theEventHandler.handleQuitEvent(exitAction);
            }
            
            exitAction.actionPerformed(null);
            response.performQuit();
            
        } else {
            response.cancelQuit();
        }
    }
    
    
    @Override
    protected final void addEventHandler(AbstractEventHandler eventHandler) {
        this.eventHandlers.add(eventHandler);
    }
    
    @Override
    public final AbstractEventHandler getEventHandler() {
        return this.eventHandler;
    }
    
    private static class EventHandler extends AbstractEventHandler<CoreController> {

        public EventHandler(CoreController coreController){
            super(coreController);
        }

        @Override
        public void handleOpenURIEvent(URI rawBitcoinURI) {
            // do nothing
        }

        @Override
        public void handleQuitEvent(ExitAction exitAction) {
            exitAction.setCoreController(super.controller);
        }
    }
    
    
    
}
