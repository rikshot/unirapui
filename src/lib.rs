#![cfg(target_os="android")]
#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_fi_unirapui_example_MainActivity_hello(env: JNIEnv, _class: JClass, input: JString) -> jstring {
    let input: String = env.get_string(input).expect("Couldn't get java string!").into();

    let output = env.new_string(format!("Greetings, {}!", input)).expect("Couldn't create java string!");

    output.into_inner()
}
