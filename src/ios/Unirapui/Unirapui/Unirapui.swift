//
//  Unirapui.swift
//  Unirapui
//
//  Created by Ville Orkas on 15.6.2021.
//

import Foundation
import SwiftUI
import WebKit
import Security

public final class WebView: NSObject, UIViewRepresentable, WKNavigationDelegate {
    
    let port: ushort
    let serverTrust: SecTrust
    let clientIdentity: SecIdentity
    
    let configuration: WKWebViewConfiguration;
    let webView: WKWebView
    
    init(port: ushort, serverTrust: SecTrust, clientIdentity: SecIdentity) {
        self.port = port
        self.serverTrust = serverTrust
        self.clientIdentity = clientIdentity
        configuration = WKWebViewConfiguration()
        webView = WKWebView(frame: .zero, configuration: configuration)
        super.init()
        webView.navigationDelegate = self
    }
    
    public func makeUIView(context: Context) -> WKWebView  {
        return webView
    }
    
    public func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.load(URLRequest(url: URL(string: "https://127.0.0.1:\(port)")!))
    }
        
    public func webView(_ webView: WKWebView, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        switch challenge.protectionSpace.authenticationMethod {
        case NSURLAuthenticationMethodServerTrust:
            let credential = URLCredential(trust: challenge.protectionSpace.serverTrust!)
            completionHandler(.useCredential, credential)
            break
        case NSURLAuthenticationMethodClientCertificate:
            let credential = URLCredential(identity: clientIdentity, certificates: nil, persistence: .forSession)
            completionHandler(.useCredential, credential)
            break
        default:
            completionHandler(.performDefaultHandling, .none)
            break
        }
    }
    
}

func getCertificate(bundle: Bundle, name: String) -> SecCertificate {
    let certUrl = bundle.url(forResource: name, withExtension: "crt")!
    var cert = String(decoding: try! FileHandle(forReadingFrom: certUrl).readToEnd()!, as: UTF8.self)
    cert = cert.replacingOccurrences(of: "-----BEGIN CERTIFICATE-----", with: "")
    cert = cert.replacingOccurrences(of: "-----END CERTIFICATE-----", with: "")
    cert = cert.replacingOccurrences(of: "\n", with: "")
    return SecCertificateCreateWithData(nil, Data(base64Encoded: cert)! as CFData)!
}

func getPrivateKey(bundle: Bundle, name: String) -> SecIdentity {
    let keyUrl = bundle.url(forResource: name, withExtension: "p12")!
    var rawItems: CFArray?
    SecPKCS12Import(try! Data(contentsOf: keyUrl) as CFData, [kSecImportExportPassphrase as String: ""] as CFDictionary, &rawItems)
    let items = rawItems! as! Array<Dictionary<String, Any>>
    let firstItem = items[0]
    return firstItem[kSecImportItemIdentity as String] as! SecIdentity
}

public class Unirapui {
    public class func create(port: ushort) throws -> WebView {
        let bundleURL = Bundle.main.url(forResource: "Resources", withExtension: "bundle", subdirectory: "Frameworks/Unirapui.framework")!
        let bundle = Bundle(url: bundleURL)!
        
        let caCert = getCertificate(bundle: bundle, name: "ca")
        let clientCert = getCertificate(bundle: bundle, name: "client")
        let clientIdentity = getPrivateKey(bundle: bundle, name: "client")
        
        let policy = SecPolicyCreateSSL(true, "127.0.0.1" as CFString)
        var serverTrust: SecTrust?
        SecTrustCreateWithCertificates([caCert, clientCert] as AnyObject, policy, &serverTrust)
        
        let indexUrl = bundle.url(forResource: "index", withExtension: "html")!
        let index = String(decoding: try! FileHandle(forReadingFrom: indexUrl).readToEnd()!, as: UTF8.self)
        
        Unirapui_start(index, port)
        return WebView(port: port, serverTrust: serverTrust!, clientIdentity: clientIdentity)
    }
}
