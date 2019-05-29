package test.xzheng.com.webservertest1;

import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

import com.vungle.warren.ui.VungleWebClient;

@Aspect
public class VungleWebViewHook {
    private static final String TAG = VungleWebViewHook.class.getSimpleName();

    @After("execution(* com.vungle.warren.ui.VungleWebClient.onPageFinished(..))")
    public void onPageFinishedAfter(JoinPoint joinPoint) throws Throwable {
        String key = joinPoint.getSignature().toString();
        Object[] args = joinPoint.getArgs();
        Log.d(TAG, "onPageFinishedAfter: " + key);
        Log.d(TAG, "args.length:" + args.length);

        //load injected js
    }
 }
