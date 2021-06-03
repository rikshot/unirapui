use warp::hyper::Response;
use warp::{ws::Message, ws::Ws, Filter};

use log::error;

use futures::SinkExt;
use futures::StreamExt;

const SERVER_KEY: &str = include_str!("../certs/server.key");
const SERVER_CERT: &str = include_str!("../certs/server.crt");
const CA_CERT: &str = include_str!("../certs/ca.crt");

pub fn echo(name: &str) -> String {
    name.to_owned()
}

pub async fn start(index: String, port: u16) {
    let root = warp::get()
        .and(warp::filters::path::end())
        .map(move || index.clone())
        .with(warp::filters::reply::header("Content-Type", "text/html"));

    let text = warp::path!("hello" / String).map(|name: String| echo(&name));
    let binary = warp::post()
        .and(warp::path("hello"))
        .and(warp::body::bytes())
        .map(|bytes| {
            Response::builder()
                .header("Content-Type", "application/octet-binary")
                .body(bytes)
                .unwrap()
        });

    let ws = warp::path("ws").and(warp::ws()).map(|ws: Ws| {
        ws.on_upgrade(|websocket| async {
            let (mut tx, mut rx) = websocket.split();
            while let Some(result) = rx.next().await {
                let message = match result {
                    Ok(message) => message,
                    Err(error) => {
                        error!("websocket error: {}", error);
                        break;
                    }
                };
                if let Ok(message) = message.to_str() {
                    tx.send(Message::text(echo(message))).await.unwrap();
                } else if message.is_binary() {
                    tx.send(Message::binary(message.as_bytes())).await.unwrap();
                }
            }
        })
    });

    warp::serve(root.or(text).or(binary).or(ws))
        .tls()
        .key(SERVER_KEY)
        .cert(SERVER_CERT)
        .client_auth_required(CA_CERT)
        .run(([127, 0, 0, 1], port))
        .await;
}
