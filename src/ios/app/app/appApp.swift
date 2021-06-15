//
//  appApp.swift
//  app
//
//  Created by Ville Orkas on 13.6.2021.
//

import SwiftUI
import WebKit

import Unirapui

@main
struct appApp: App {

    var body: some Scene {
        WindowGroup {
            try? Unirapui.create(port: 8000)
        }
    }
}
