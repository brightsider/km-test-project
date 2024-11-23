//
//  SceneDelegate.swift
//  iosApp
//
//  Created by Vlad Hetman on 23.11.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import BackgroundTasks
import Shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    
    func scene(_ scene: UIScene, willConnectTo
               session: UISceneSession, options
               connectionOptions: UIScene.ConnectionOptions) {
        guard let _ = scene as? UIWindowScene else { return }
    }
    
    func sceneDidEnterBackground(_ scene: UIScene) {
        backgroundTask = UIApplication.shared.beginBackgroundTask(withName: "QueueProcessing") {
            UIApplication.shared.endBackgroundTask(self.backgroundTask)
            self.backgroundTask = .invalid
        }
        
        HttpRequestManager.shared.processQueues()
    }
    
    func sceneDidBecomeActive(_ scene: UIScene) {
        if backgroundTask != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTask)
            backgroundTask = .invalid
        }
    }
}
