#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use std::ffi::c_void;

use log::Level;

extern crate android_logger;
use android_logger::Config;

use jni::JavaVM;
use jni::objects::{JClass, JString};
use jni::JNIEnv;
use jni::sys::JNI_VERSION_1_6;
use jni::sys::{jint, jshort, jstring};

use tokio::runtime::Runtime;

static mut RUNTIME: Option<Runtime> = None;

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: JavaVM, _reserved: *mut c_void) -> jint {
    android_logger::init_once(Config::default().with_min_level(Level::Debug).with_tag("unirapui"));
    log::debug!("JNI_OnLoad");
    unsafe { RUNTIME = Some(Runtime::new().expect("Unable to create tokio runtime")); }
    JNI_VERSION_1_6
}

// If this is ever even called, drops the tokio runtime as static variables do not call drop automagically
#[no_mangle]
pub extern "system" fn JNI_OnUnload(_vm: JavaVM, _reserved: *mut c_void) {
    if let Some(runtime) = unsafe { RUNTIME.as_ref() } {
        drop(runtime);
    }
}

#[no_mangle]
pub extern "system" fn Java_fi_unirapui_lib_Unirapui_start(env: JNIEnv, _class: JClass, index: JString, port: jshort) {
    let index = env.get_string(index).expect("Couldn't get java string").into();    
    if let Some(runtime) = unsafe { RUNTIME.as_ref() } {
        runtime.spawn(crate::unirapui::start(index, port as u16));
    }
}

#[no_mangle]
pub extern "system" fn Java_fi_unirapui_lib_Unirapui_greeting(env: JNIEnv, _class: JClass, name: JString) -> jstring {
    let name: String =
        env.get_string(name).expect("Couldn't get java string!").into();
    let output = env.new_string(crate::unirapui::greeting(&name))
        .expect("Couldn't create java string!");
    output.into_inner()
}