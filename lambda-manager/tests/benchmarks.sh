#!/bin/bash

DATA_IP="172.18.0.1"
DATA_PORT=8000
DATA_ADDRESS="http://$DATA_IP:$DATA_PORT"

BASE_BENCHMARKS=(bfs mst pagerank uploader compression dna thumbnailer dynamic-html video-processing classify)

NA_NA_BENCHMARKS=()
NA_DE_BENCHMARKS=()
MO_BENCHMARKS=()
for bench in "${BASE_BENCHMARKS[@]}"; do
    NA_NA_BENCHMARKS+=("na_na_$bench")
    NA_DE_BENCHMARKS+=("na_de_$bench")
    MO_BENCHMARKS+=("mo_$bench")
done


declare -A BENCHMARK_CODE
declare -A BENCHMARK_METADATA
declare -A BENCHMARK_PAYLOADS

# Helper strings for trampoline URLs.
TRAMPOLINE_BASE="$DATA_ADDRESS/apps/trampoline/native"
HTTP_TRAMPOLINE='{"url": "'$TRAMPOLINE_BASE'/libhttp_trampoline.so", "functions": [{"module": "env", "name": "host_download", "param_types": ["i32", "i32", "i32", "i32"]}]}'
HTTP_FILE_TRAMPOLINE='{"url": "'$TRAMPOLINE_BASE'/libhttp_trampoline.so", "functions": [{"module": "env", "name": "host_download_to_file", "param_types": ["i32", "i32", "i32", "i32"]}]}'
HTTP_BOTH_TRAMPOLINE='{"url": "'$TRAMPOLINE_BASE'/libhttp_trampoline.so", "functions": [{"module": "env", "name": "host_download", "param_types": ["i32", "i32", "i32", "i32"]}, {"module": "env", "name": "host_download_to_file", "param_types": ["i32", "i32", "i32", "i32"]}]}'

for bench in "${BASE_BENCHMARKS[@]}"; do
    # Replacing hyphens with underscores for library files (for .wasm and .so files convention).
    bench_underscores="${bench//-/_}"

    # Native configuration.
    BENCHMARK_CODE["na_na_$bench"]="$DATA_ADDRESS/apps/native/native/lib$bench_underscores.so"
    BENCHMARK_METADATA["na_na_$bench"]='{}'
    BENCHMARK_CODE["na_de_$bench"]="$DATA_ADDRESS/apps/native/default/lib$bench_underscores.so"
    BENCHMARK_METADATA["na_de_$bench"]='{}'

    # Mosaic configuration.
    BENCHMARK_CODE["mo_$bench"]="$DATA_ADDRESS/apps/wasm/$bench_underscores.wasm"

    # Payloads.
    for prefix in "na_na" "na_de" "mo"; do
        full_bench="${prefix}_${bench}"

        # Determine FFmpeg binary based on mode.
        if [[ "$prefix" == "mo" || "$prefix" == "na_na" ]]; then
            FFMPEG_URL="$DATA_ADDRESS/ffmpeg_optimized"
        else
            FFMPEG_URL="$DATA_ADDRESS/ffmpeg"
        fi


        case $bench in
            bfs|mst)
                payload='{"size": 100000, "m": 10}'
                ;;
            pagerank)
                payload='{"size": 10000, "m": 10, "iterations": 20}'
                ;;
            uploader)
                payload='{"download_url": "'$DATA_ADDRESS'/video.mp4", "upload_url": "http://172.18.0.1:9696/upload"}'
                ;;
            compression)
                payload='{"input_url": "'$DATA_ADDRESS'/video.mp4"}'
                ;;
            dna)
                payload='{"url": "'$DATA_ADDRESS'/bacillus_subtilis.fasta"}'
                ;;
            thumbnailer)
                payload='{"url": "'$DATA_ADDRESS'/snap.png", "target_width": 200, "target_height": 200}'
                ;;
            dynamic-html)
                payload='{"url": "'$DATA_ADDRESS'/template.html", "username": "rbruno", "random_len": 1000000}'
                ;;
            video-processing)
                payload='{"video_url": "'$DATA_ADDRESS'/video.mp4", "watermark_url": "'$DATA_ADDRESS'/watermark.png", "ffmpeg_url": "'$FFMPEG_URL'"}'
                ;;
            classify)
                payload='{"model_url": "'$DATA_ADDRESS'/resnet50.onnx", "image_url": "'$DATA_ADDRESS'/eagle.jpg", "labels_url": "'$DATA_ADDRESS'/resnet_labels.txt"}'
                ;;
        esac

        BENCHMARK_PAYLOADS["$full_bench"]=$payload
    done
done

# Mosaic trampoline metadata.
BENCHMARK_METADATA["mo_bfs"]='[{"url": "'$TRAMPOLINE_BASE'/libpetgraph_trampoline.so", "functions": [{"module": "env", "name": "host_bfs", "param_types": ["i32", "i32", "i32", "i32", "i32", "i32"]}]}]'

BENCHMARK_METADATA["mo_mst"]='[{"url": "'$TRAMPOLINE_BASE'/libpetgraph_trampoline.so", "functions": [{"module": "env", "name": "host_mst", "param_types": ["i32", "i32", "i32", "i32", "i32"]}]}]'

BENCHMARK_METADATA["mo_pagerank"]='[{"url": "'$TRAMPOLINE_BASE'/libpagerank_trampoline.so", "functions": [{"module": "env", "name": "host_pagerank", "param_types": ["i32", "i32", "i32", "i32", "i32"]}]}]'

BENCHMARK_METADATA["mo_uploader"]='[{"url": "'$TRAMPOLINE_BASE'/libhttp_trampoline.so", "functions": [{"module": "env", "name": "host_download", "param_types": ["i32", "i32", "i32", "i32"]}, {"module": "env", "name": "host_upload", "param_types": ["i32", "i32", "i32", "i32", "i32", "i32"]}]}]'

BENCHMARK_METADATA["mo_compression"]="[$HTTP_TRAMPOLINE, {\"url\": \"$TRAMPOLINE_BASE/libcompression_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_compress\", \"param_types\": [\"i32\", \"i32\", \"i32\", \"i32\"]}]}]"

BENCHMARK_METADATA["mo_dna"]="[$HTTP_TRAMPOLINE, {\"url\": \"$TRAMPOLINE_BASE/libsquiggle_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_squiggle_transform\", \"param_types\": [\"i32\", \"i32\", \"i32\", \"i32\"]}]}]"

BENCHMARK_METADATA["mo_thumbnailer"]="[$HTTP_TRAMPOLINE, {\"url\": \"$TRAMPOLINE_BASE/libthumbnailer_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_resize\", \"param_types\": [\"i32\", \"i32\", \"i32\", \"i32\", \"i32\", \"i32\"]}]}]"

BENCHMARK_METADATA["mo_dynamic-html"]="[$HTTP_TRAMPOLINE, {\"url\": \"$TRAMPOLINE_BASE/libhtml_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_render\", \"param_types\": [\"i32\", \"i32\", \"i32\", \"i32\", \"i32\", \"i32\", \"i32\", \"i32\"]}]}]"

BENCHMARK_METADATA["mo_video-processing"]="[$HTTP_FILE_TRAMPOLINE, {\"url\": \"$TRAMPOLINE_BASE/libcli_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_run_command\", \"param_types\": [\"i32\", \"i32\"]}]}, {\"url\": \"$TRAMPOLINE_BASE/libfs_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_file_exists\", \"param_types\": [\"i32\", \"i32\"]}]}]"

BENCHMARK_METADATA["mo_classify"]="[$HTTP_BOTH_TRAMPOLINE, {\"url\": \"$TRAMPOLINE_BASE/libclassify_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_infer\", \"param_types\": [\"i32\", \"i32\", \"i32\", \"i32\"]}]}, {\"url\": \"$TRAMPOLINE_BASE/libfs_trampoline.so\", \"functions\": [{\"module\": \"env\", \"name\": \"host_file_exists\", \"param_types\": [\"i32\", \"i32\"]}, {\"module\": \"env\", \"name\": \"host_read_file\", \"param_types\": [\"i32\", \"i32\", \"i32\", \"i32\"]}]}]"
