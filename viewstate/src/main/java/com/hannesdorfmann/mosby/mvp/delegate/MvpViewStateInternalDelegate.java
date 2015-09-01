/*
 * Copyright 2015 Hannes Dorfmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hannesdorfmann.mosby.mvp.delegate;

import android.os.Bundle;
import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.hannesdorfmann.mosby.mvp.viewstate.RestoreableViewState;
import com.hannesdorfmann.mosby.mvp.viewstate.ViewState;

/**
 * This class
 * is used to save, restore and apply a {@link ViewState} by using {@link
 * BaseMvpViewStateDelegateCallback}. It's
 * just a little helper / utils class that avoids to many copy & paste code clones.
 * However, you have to hook in the corresponding methods correctly in your Fragment, Activity or
 * android.widget.View
 *
 * @author Hannes Dorfmann
 * @since 1.0.0
 */
public class MvpViewStateInternalDelegate<V extends MvpView, P extends MvpPresenter<V>>
    extends MvpInternalDelegate<V, P> {

  private boolean applyViewState = false;

  public MvpViewStateInternalDelegate(BaseMvpViewStateDelegateCallback<V, P> delegateCallback) {
    super(delegateCallback);
  }

  /**
   * Like the name already suggests. Creates a new viewstate or tries to restore the old one (must
   * be subclass of {@link RestoreableViewState}) by reading the bundle
   *
   * @return true, if the viewstate has been restored (in other words {@link
   * ViewState#apply(MvpView, boolean)} has been invoked) (calls {@link
   * BaseMvpViewStateDelegateCallback#onViewStateInstanceRestored(boolean) after having restored the
   * viewstate}.
   * Otherwise returns false and calls {@link BaseMvpViewStateDelegateCallback#onNewViewStateInstance()}
   */
  public boolean createOrRestoreViewState(Bundle savedInstanceState) {

    BaseMvpViewStateDelegateCallback<V, P> viewStateSupport =
        (BaseMvpViewStateDelegateCallback<V, P>) delegateCallback;

    // ViewState already exists (Fragment retainsInstanceState == true)
    if (viewStateSupport.getViewState() != null) {
      applyViewState = true;
      return true;
    }

    // Create view state
    viewStateSupport.setViewState(viewStateSupport.createViewState());
    if (viewStateSupport.getViewState() == null) {
      throw new NullPointerException(
          "ViewState is null! Do you return null in createViewState() method?");
    }

    // Try to restore data from bundle (savedInstanceState)
    if (savedInstanceState != null
        && viewStateSupport.getViewState() instanceof RestoreableViewState) {

      ViewState restoredViewState =
          ((RestoreableViewState) viewStateSupport.getViewState()).restoreInstanceState(
              savedInstanceState);

      boolean restoredFromBundle = restoredViewState != null;

      if (restoredFromBundle) {
        viewStateSupport.setViewState(restoredViewState);
        applyViewState = true;
        return true;
      }
    }

    // ViewState not restored, activity / fragment starting first time
    applyViewState = false;
    return false;
  }

  /**
   * applies the views state to the view.
   *
   * @return true if viewstate has been applied, otherwise false (i.e. activity / fragment is
   * starting for the first time)
   */
  public boolean applyViewState() {

    BaseMvpViewStateDelegateCallback<V, P> delegate =
        (BaseMvpViewStateDelegateCallback<V, P>) delegateCallback;

    if (applyViewState) {
      boolean retainingInstance = delegateCallback.isRetainingInstance();
      delegate.setRestoringViewState(true);
      delegate.getViewState().apply(delegate.getMvpView(), retainingInstance);
      delegate.setRestoringViewState(false);
      delegate.onViewStateInstanceRestored(retainingInstance);
      return true;
    }

    delegate.onNewViewStateInstance();
    return false;
  }

  /**
   * Saves {@link RestoreableViewState} in a bundle. <b>Should be calld from activities or
   * fragments
   * onSaveInstanceState(Bundle) method</b>
   */
  public void saveViewState(Bundle outState) {

    BaseMvpViewStateDelegateCallback<V, P> delegate =
        (BaseMvpViewStateDelegateCallback<V, P>) delegateCallback;


    if (delegate == null) {
      throw new NullPointerException("ViewStateSupport can not be null");
    }

    ViewState viewState = delegate.getViewState();
    if (viewState == null) {
      throw new NullPointerException("ViewState is null! That's not allowed");
    }

    boolean retainingInstanceState = delegate.isRetainingInstance();


    // Save the viewstate
    if (viewState != null && viewState instanceof RestoreableViewState) {
      ((RestoreableViewState) viewState).saveInstanceState(outState);
    }

    if (retainingInstanceState) {
      // For next time we call applyViewState() we have to set this flag to true
      applyViewState = true;
    }
  }
}
