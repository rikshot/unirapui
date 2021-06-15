#![cfg(target_os = "ios")]

use std::ffi::CStr;
use std::os::raw::{c_char, c_ushort};

use tokio::runtime::Runtime;

static mut RUNTIME: Option<Runtime> = None;

#[no_mangle]
pub unsafe extern "C" fn Unirapui_start(index: *const c_char, port: c_ushort) {
    env_logger::Builder::from_default_env()
        .filter_level(log::LevelFilter::Info)
        .init();
    RUNTIME = Some(Runtime::new().expect("Unable to create tokio runtime"));
    if let Some(runtime) = RUNTIME.as_ref() {
        let index = CStr::from_ptr(index).to_string_lossy().into_owned();
        runtime.spawn(crate::unirapui::start(index, port as u16));
    }
}
