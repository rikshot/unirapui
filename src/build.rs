extern crate cbindgen;

use std::env;
use std::error::Error;
use std::path::PathBuf;

fn main() -> Result<(), Box<dyn Error>> {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let output_file = PathBuf::from("src/ios/Unirapui/Unirapui/Unirapui.h")
        .display()
        .to_string();
    cbindgen::generate(&crate_dir)?.write_to_file(&output_file);
    Ok(())
}
