package utils;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * This is a skeleton to illustrate how to create a 'service' that
 * can be started, cancelled, and connected to a progress bar with the
 * 'service' executing on a background thread, not the FX GUI thread.
 * 
 * In this template, we choose to make the result (answer) of the service
 * available through a specific 'getter' rather than have the Task return
 * a result that is available via service.getResult() technique.
 * 
 * In practice this class is put inside the main class (presumed a singleton)
 * which would create and make publicly available a reference...for example
 * 
 * public ServiceTemple theService = new ServiceTemplate();
 * 
 * would be an instance variable of the main class.
 * 
 */

public class ServiceTemplate extends Service<Void> {

    // private variables go here --- simple examples
    double answer;
    int innerLoopCount;
    int outerLoopCount;
    double someParameter;

    // setters for private variable go here
    public final void setinnerLoopCount(int innerLoopCount) {
        this.innerLoopCount = innerLoopCount;
    }
    public final void setouterLoopCount(int outerLoopCount) {
        this.outerLoopCount = outerLoopCount;
    }
    public final void setsomeParameter(double someParameter) {
        this.someParameter = someParameter;
    }

    // getters go here
    public final double getAnswer() {
        return answer;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                answer = 0.0;
                int maxLoopCount = innerLoopCount * outerLoopCount;
                int loopCount = 0;
                search:
                for (int i = 0; i < outerLoopCount; i++) {
                    for (int k = 0; k < innerLoopCount; i++) {
                        if (isCancelled()) {
                            break search;
                        }
                        // Include code like the following two lines
                        // if you want to be able to 'bind' a progress
                        // bar to this service's progressProperty() like this ...
                        // someProgressBar.progressProperty().bind(jocularMain.serviceTemplate.progressProperty());
                        loopCount++;
                        updateProgress(loopCount, maxLoopCount);
                        // Here we do the time consuming calculations to
                        // produce 'answer' --- for example :)
                        answer += someParameter;
                    }
                }
                return null;
            }
        };
    }
}
