use warp::Filter;

const SERVER_KEY: &str = include_str!("../certs/server.key");
const SERVER_CERT: &str = include_str!("../certs/server.crt");
const CA_CERT: &str = include_str!("../certs/ca.crt");

pub async fn start(port: u16) {
    let hello = warp::path!("hello" / String).map(|name| format!("Hello, {}!", name));
    warp::serve(hello)
        .tls()
        .key(SERVER_KEY)
        .cert(SERVER_CERT)
        .client_auth_required(CA_CERT)
        .run(([127, 0, 0, 1], port)).await;
}

#[cfg(test)]
mod tests {
    #[test]
    fn start() {
        let runtime = tokio::runtime::Runtime::new().unwrap();
        runtime.block_on(super::start(8000));
    }

}
